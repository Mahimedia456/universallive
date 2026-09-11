package com.universallive.app.features.system

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.universallive.app.streaming.capture.CaptureController
import com.universallive.app.streaming.state.StreamConfigState
import com.universallive.app.theme.*
import kotlinx.coroutines.launch

private val PhaseShape = RoundedCornerShape(18.dp)

@Composable
private fun PhasePage(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .systemBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            UlPageHeader(title = title, subtitle = subtitle, onBack = onBack)
            Spacer(Modifier.height(20.dp))
            content()
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    body: String,
    tone: Color = AppPrimary,
    badge: String? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PhaseShape)
            .background(AppSurface)
            .border(1.dp, AppBorder, PhaseShape)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(tone))
            Spacer(Modifier.width(9.dp))
            Text(title, color = AppText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (!badge.isNullOrBlank()) UlStatusBadge(badge, tone)
        }
        Spacer(Modifier.height(7.dp))
        Text(body, color = AppTextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@Composable
private fun SmallStatusRow(
    label: String,
    value: String,
    good: Boolean? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppSurface)
            .border(1.dp, AppBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = AppTextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(
            value,
            color = when (good) {
                true -> AppSuccess
                false -> AppWarning
                null -> AppText
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// -----------------------------------------------------------------------------
// Phase 34 - Support
// -----------------------------------------------------------------------------

@Composable
fun Phase34SupportHubScreen(
    state: MobileIntegrationState,
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { state.refreshSupportTickets() }

    PhasePage(
        title = "Help & Support",
        subtitle = "Guides, troubleshooting and real support requests in one place.",
        onBack = onBack,
    ) {
        UlTextField(
            value = query,
            onValueChange = { query = it },
            label = "Search help",
            placeholder = "Streaming, audio, connections...",
        )
        Spacer(Modifier.height(14.dp))

        val actions = listOf(
            Triple("LIVE", "Streaming Troubleshooting", "Black screen, audio, lag and publishing issues"),
            Triple("LINK", "Connection Help", "Destination setup, test and RTMP guidance"),
            Triple("DIAG", "Diagnostics", "Inspect backend, capture, permissions and stream state"),
            Triple("MAIL", "Contact Support", "Create a real support ticket through the backend"),
        ).filter {
            query.isBlank() || it.second.contains(query, ignoreCase = true) || it.third.contains(query, ignoreCase = true)
        }

        actions.forEach { item ->
            val route = when (item.second) {
                "Streaming Troubleshooting" -> AppRoute.Troubleshooting
                "Connection Help" -> AppRoute.Connections
                "Diagnostics" -> AppRoute.Diagnostics
                else -> AppRoute.ContactSupport
            }
            UlActionRow(
                marker = item.first.take(1),
                title = item.second,
                subtitle = item.third,
                onClick = { onRoute(route) },
            )
            Spacer(Modifier.height(9.dp))
        }

        Spacer(Modifier.height(10.dp))
        Text("RECENT SUPPORT TICKETS", color = AppPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(9.dp))

        when {
            state.accountLoading && state.supportTickets.isEmpty() -> UlLoadingState("Loading support", "Checking your recent support requests.")
            state.supportTickets.isEmpty() -> UlEmptyState(
                title = "No support tickets yet",
                body = "If you run into a problem, create a support request and its status will appear here.",
                actionText = "Contact Support",
                onAction = { onRoute(AppRoute.ContactSupport) },
            )
            else -> state.supportTickets.take(5).forEach { ticket ->
                InfoCard(
                    title = ticket.subject,
                    body = "${ticket.category} • ${ticket.description}",
                    tone = if (ticket.status.equals("closed", true) || ticket.status.equals("resolved", true)) AppSuccess else AppPrimary,
                    badge = ticket.status.uppercase(),
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        state.error?.let { message ->
            Spacer(Modifier.height(12.dp))
            UlErrorState(
                title = "Support could not refresh",
                body = message,
                onRetry = { scope.launch { state.refreshSupportTickets() } },
            )
        }
    }
}

@Composable
fun Phase34TroubleshootingScreen(
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    PhasePage(
        title = "Streaming Troubleshooting",
        subtitle = "Target the most common broadcast problems without guessing.",
        onBack = onBack,
    ) {
        InfoCard("Black or frozen video", "Re-check screen-capture consent, protected-content limitations, orientation and encoder readiness.", AppWarning)
        Spacer(Modifier.height(9.dp))
        UlActionRow("Q", "Check Stream Quality", "Review resolution, FPS, bitrate and device readiness", onClick = { onRoute(AppRoute.QualityCenter) })
        Spacer(Modifier.height(9.dp))
        InfoCard("No microphone / distorted audio", "Confirm microphone permission, source toggles and test game audio separately from microphone capture.", AppWarning)
        Spacer(Modifier.height(9.dp))
        UlActionRow("A", "Open Streaming Settings", "Review microphone, internal audio and video defaults", onClick = { onRoute(AppRoute.StreamingSettings) })
        Spacer(Modifier.height(9.dp))
        InfoCard("Destination will not publish", "Test the saved destination and verify that server URL / stream key credentials are current.", AppWarning)
        Spacer(Modifier.height(9.dp))
        UlActionRow("C", "Manage Connections", "Test, edit or replace destination credentials", onClick = { onRoute(AppRoute.Connections) })
        Spacer(Modifier.height(9.dp))
        UlActionRow("D", "Open Diagnostics", "Inspect current runtime and backend state", onClick = { onRoute(AppRoute.Diagnostics) })
    }
}

@Composable
fun Phase34ContactSupportScreen(
    state: MobileIntegrationState,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var category by remember { mutableStateOf("Streaming") }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    PhasePage(
        title = "Contact Support",
        subtitle = "Send a real support request to the Universal Live backend.",
        onBack = onBack,
    ) {
        Text("ISSUE CATEGORY", color = AppPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("Streaming", "Account", "Billing").forEach { item ->
                FilterChip(
                    selected = category == item,
                    onClick = { category = item },
                    label = { Text(item, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AppPrimary.copy(alpha = .18f)),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        UlTextField(value = subject, onValueChange = { subject = it }, label = "Subject", placeholder = "Short summary")
        Spacer(Modifier.height(10.dp))
        UlTextField(value = description, onValueChange = { description = it }, label = "Description", placeholder = "Describe what happened")
        Spacer(Modifier.height(14.dp))

        InfoCard(
            title = "Privacy-aware support",
            body = "Authentication tokens, stream keys and passwords are never shown or copied into this form. Runtime diagnostics can be reviewed separately before you contact support.",
            tone = AppPrimary,
        )
        Spacer(Modifier.height(14.dp))

        if (submitted) {
            UlSuccessState(
                title = "Support request sent",
                body = "Your request was accepted by the backend. You can review its status from Help & Support.",
            )
            Spacer(Modifier.height(12.dp))
        }

        state.error?.let {
            UlErrorState(
                title = "Request not sent",
                body = it,
                onRetry = { state.clearError() },
            )
            Spacer(Modifier.height(12.dp))
        }

        UlPrimaryButton(
            text = "Submit Support Request",
            onClick = {
                scope.launch {
                    submitted = state.submitSupportTicket(category, subject.trim(), description.trim())
                    if (submitted) {
                        subject = ""
                        description = ""
                    }
                }
            },
            enabled = subject.isNotBlank() && description.isNotBlank(),
            loading = state.accountLoading,
        )
    }
}

// -----------------------------------------------------------------------------
// Phase 35 - Diagnostics
// -----------------------------------------------------------------------------

@Composable
fun Phase35DiagnosticsScreen(
    state: MobileIntegrationState,
    streamState: StreamConfigState,
    captureController: CaptureController,
    permissions: PermissionSetupController,
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var healthRun by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    val snapshot = captureController.snapshot
    val config = streamState.config
    val permissionSnapshot = permissions.snapshot

    PhasePage(
        title = "Diagnostics",
        subtitle = "Inspect the current app, backend and streaming runtime without exposing secrets.",
        onBack = onBack,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UlMetricTile("Connections", state.connections.size.toString(), Modifier.weight(1f))
            UlMetricTile("Scenes", state.scenes.size.toString(), Modifier.weight(1f))
            UlMetricTile("Capture", snapshot.status.name, Modifier.weight(1f), if (snapshot.isActive) AppSuccess else AppTextSecondary)
        }
        Spacer(Modifier.height(14.dp))

        Text("ACCOUNT & BACKEND", color = AppPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        SmallStatusRow("Authenticated", if (state.isAuthenticated) "Yes" else "No", state.isAuthenticated)
        Spacer(Modifier.height(7.dp))
        SmallStatusRow("Profile", state.profile?.displayName ?: "Not loaded", state.profile != null)
        Spacer(Modifier.height(7.dp))
        SmallStatusRow("Membership", state.membership?.planName ?: "Not loaded", state.membership != null)
        Spacer(Modifier.height(7.dp))
        SmallStatusRow("Backend", state.api.baseUrl.ifBlank { "Unavailable" }, state.api.baseUrl.isNotBlank())
        state.finalDiagnostics?.let { diag ->
            Spacer(Modifier.height(7.dp))
            SmallStatusRow("Backend contract", diag.mobileContractVersion, diag.ok)
            Spacer(Modifier.height(7.dp))
            SmallStatusRow("Firebase push", if (diag.firebaseConfigured) "Configured" else "Not configured", diag.firebaseConfigured)
            Spacer(Modifier.height(7.dp))
            SmallStatusRow("SMTP email", if (diag.smtpConfigured) "Configured" else "Console / unavailable", diag.smtpConfigured)
            state.diagnosticSnapshotId?.let { snapshotId ->
                Spacer(Modifier.height(7.dp))
                SmallStatusRow("Support snapshot", snapshotId.take(8) + "…", true)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("STREAM RUNTIME", color = AppPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        SmallStatusRow("Video", "${config.resolution.label} • ${config.fps.value} FPS • ${config.bitrateKbps} Kbps")
        Spacer(Modifier.height(7.dp))
        SmallStatusRow("Publisher", snapshot.publishStatus.name, snapshot.isPublishing)
        Spacer(Modifier.height(7.dp))
        SmallStatusRow("Encoder", snapshot.encoderName.ifBlank { "Not active" }, snapshot.encoderName.isNotBlank())
        Spacer(Modifier.height(7.dp))
        SmallStatusRow("Output", if (snapshot.encoderWidth > 0) "${snapshot.encoderWidth}×${snapshot.encoderHeight}" else "Not active")
        Spacer(Modifier.height(7.dp))
        SmallStatusRow("Network bitrate", if (snapshot.networkBitrateBps > 0) "${snapshot.networkBitrateBps / 1000} Kbps" else "No live telemetry")

        Spacer(Modifier.height(16.dp))
        Text("PERMISSIONS", color = AppPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        SmallStatusRow("Microphone", permissionSnapshot.microphone.name, permissionSnapshot.microphone == PermissionGrantState.GRANTED)
        Spacer(Modifier.height(7.dp))
        SmallStatusRow("Camera", permissionSnapshot.camera.name, permissionSnapshot.camera == PermissionGrantState.GRANTED || permissionSnapshot.camera == PermissionGrantState.NOT_REQUIRED)
        Spacer(Modifier.height(7.dp))
        SmallStatusRow("Notifications", permissionSnapshot.notifications.name, permissionSnapshot.notifications == PermissionGrantState.GRANTED || permissionSnapshot.notifications == PermissionGrantState.NOT_REQUIRED)
        Spacer(Modifier.height(7.dp))
        SmallStatusRow("Screen capture education", if (permissionSnapshot.screenCaptureEducationComplete) "Complete" else "Pending", permissionSnapshot.screenCaptureEducationComplete)

        Spacer(Modifier.height(16.dp))
        if (healthRun) {
            InfoCard(
                title = if (state.error == null) "Health check complete" else "Health check needs attention",
                body = state.error ?: "Account, destinations, notifications and history endpoints responded through the current backend contract.",
                tone = if (state.error == null) AppSuccess else AppWarning,
                badge = if (state.error == null) "PASS" else "REVIEW",
            )
            Spacer(Modifier.height(12.dp))
        }

        UlPrimaryButton(
            text = "Run Health Check",
            onClick = {
                scope.launch {
                    running = true
                    state.clearError()
                    state.refreshAccount()
                    state.refreshConnections()
                    state.refreshNotifications()
                    state.refreshHistory()
                    state.refreshSystemState()
                    state.refreshFinalDiagnostics(
                        appVersion = "0.40.0",
                        buildNumber = "39",
                        captureStatus = snapshot.status.name,
                        publishStatus = snapshot.publishStatus.name,
                        persistSnapshot = true,
                    )
                    healthRun = true
                    running = false
                }
            },
            loading = running,
        )
        Spacer(Modifier.height(9.dp))
        UlSecondaryButton(text = "Open Global UI Audit", onClick = { onRoute(AppRoute.GlobalComponents) })
        Spacer(Modifier.height(9.dp))
        UlSecondaryButton(text = "Run End-to-End QA", onClick = { onRoute(AppRoute.FunctionalQa) })
        Spacer(Modifier.height(9.dp))
        UlSecondaryButton(text = "Contact Support", onClick = { onRoute(AppRoute.ContactSupport) })
    }
}

// -----------------------------------------------------------------------------
// Phase 36 - Legal / About
// -----------------------------------------------------------------------------

@Composable
fun Phase36LegalAboutScreen(
    state: MobileIntegrationState,
    onBack: () -> Unit,
) {
    var expanded by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { state.refreshLegal() }

    PhasePage(
        title = "About & Legal",
        subtitle = "Application information and the current backend-managed legal documents.",
        onBack = onBack,
    ) {
        val about = state.legalAbout
        InfoCard(
            title = about?.appName ?: "Universal Live",
            body = "Mobile multi-platform streaming workspace • version ${about?.appVersion ?: "0.40.0"}",
            tone = AppPrimary,
            badge = "BUILD ${about?.buildNumber ?: 39}",
        )
        Spacer(Modifier.height(8.dp))
        SmallStatusRow("API contract", about?.mobileContractVersion ?: "2026.09-final", about != null)
        Spacer(Modifier.height(14.dp))

        when {
            state.accountLoading && state.legalDocuments.isEmpty() ->
                UlLoadingState("Loading legal documents", "Reading the active legal versions from Universal Live backend.")
            state.legalDocuments.isEmpty() ->
                UlEmptyState("Legal documents unavailable", "Retry when the backend connection is available.", "Retry") {
                    scope.launch { state.refreshLegal() }
                }
            else -> state.legalDocuments.forEach { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppSurface)
                        .border(1.dp, AppBorder, RoundedCornerShape(16.dp))
                        .clickable { expanded = if (expanded == item.documentKey) null else item.documentKey }
                        .padding(15.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.title, color = AppText, fontWeight = FontWeight.SemiBold)
                            Text("Version ${item.version}", color = AppTextMuted, fontSize = 10.sp)
                        }
                        if (item.requiredAcceptance) UlStatusBadge("REQUIRED", AppWarning)
                        Spacer(Modifier.width(8.dp))
                        Text(if (expanded == item.documentKey) "−" else "+", color = AppPrimary, fontSize = 20.sp)
                    }
                    if (expanded == item.documentKey) {
                        Spacer(Modifier.height(8.dp))
                        Text(item.body, color = AppTextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
                        if (item.requiredAcceptance && state.isAuthenticated) {
                            Spacer(Modifier.height(10.dp))
                            UlSecondaryButton(
                                text = "Accept ${item.title}",
                                onClick = { scope.launch { state.acceptLegalDocument(item.documentKey, item.version) } },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(9.dp))
            }
        }

        state.error?.let { message ->
            Spacer(Modifier.height(10.dp))
            UlErrorState(
                title = "Legal service needs attention",
                body = message,
                onRetry = { scope.launch { state.refreshLegal() } },
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Phase 37 - Global Empty / Error / Loading States
// -----------------------------------------------------------------------------

@Composable
fun Phase37OfflineScreen(onRetry: () -> Unit, onHome: () -> Unit) {
    PhasePage("You're Offline", "Internet access is required for account sync and publishing.") {
        UlStatePanel(
            title = "No internet connection",
            body = "Local scene work can continue, but destinations, account sync and live publishing need a network connection.",
            tone = UlStateTone.WARNING,
            marker = "!",
            primaryText = "Retry Connection",
            onPrimary = onRetry,
            secondaryText = "Go Home",
            onSecondary = onHome,
        )
    }
}

@Composable
fun Phase37SessionExpiredScreen(onSignIn: () -> Unit) {
    PhasePage("Session Expired", "Sign in again to continue securely.") {
        UlStatePanel(
            title = "Your session ended",
            body = "Universal Live keeps supported local state while you authenticate again. No password or stream key is retained in this screen.",
            tone = UlStateTone.WARNING,
            marker = "A",
            primaryText = "Sign In Again",
            onPrimary = onSignIn,
        )
    }
}

@Composable
fun Phase37PermissionBlockedScreen(onBack: () -> Unit) {
    PhasePage("Permission Required", "A device capability required by the selected feature is unavailable.", onBack) {
        UlStatePanel(
            title = "Permission blocked",
            body = "Review microphone, camera or notification permissions in Android/iOS settings. Screen-capture consent is requested by the operating system when a broadcast begins.",
            tone = UlStateTone.WARNING,
            marker = "!",
            secondaryText = "Not Now",
            onSecondary = onBack,
        )
    }
}

@Composable
fun Phase37ServiceErrorScreen(onRetry: () -> Unit, onHome: () -> Unit) {
    PhasePage("Service Unavailable", "The last request could not be completed.") {
        UlErrorState(
            title = "Temporary service problem",
            body = "Retry the operation. If the problem continues, open Diagnostics or contact Support.",
            onRetry = onRetry,
            secondaryText = "Go Home",
            onSecondary = onHome,
        )
    }
}

// -----------------------------------------------------------------------------
// Phase 38 - Navigation + Global Components
// -----------------------------------------------------------------------------

@Composable
fun Phase38GlobalComponentsScreen(
    state: MobileIntegrationState,
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    var demoToggle by remember { mutableStateOf(true) }
    var demoChecked by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { state.refreshSystemState() }

    PhasePage(
        title = "Global UI Audit",
        subtitle = "Shared Universal Live navigation, controls and production states.",
        onBack = onBack,
    ) {
        Text("BUTTONS", color = AppPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        UlPrimaryButton(text = "Primary Action", onClick = {})
        Spacer(Modifier.height(8.dp))
        UlSecondaryButton(text = "Secondary Action", onClick = {})
        Spacer(Modifier.height(8.dp))
        UlPrimaryButton(text = "Disabled Action", onClick = {}, enabled = false)

        Spacer(Modifier.height(18.dp))
        Text("STATUS & SELECTION", color = AppPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            UlStatusBadge("READY", AppSuccess)
            UlStatusBadge("LIVE", AppLive)
            UlStatusBadge("CHECK", AppWarning)
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = demoChecked,
                onCheckedChange = { demoChecked = it },
                colors = CheckboxDefaults.colors(checkedColor = AppPrimary),
            )
            Text("Checkbox state", color = AppText, modifier = Modifier.weight(1f))
            Switch(
                checked = demoToggle,
                onCheckedChange = { demoToggle = it },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AppPrimary),
            )
        }

        Spacer(Modifier.height(18.dp))
        Text("GLOBAL STATES", color = AppPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        UlLoadingState("Loading state", "Use for blocking data that cannot yet be shown safely.")
        Spacer(Modifier.height(9.dp))
        UlEmptyState("Empty state", "Explain why there is no content and give the user a useful next action.")
        Spacer(Modifier.height(9.dp))
        UlSuccessState("Success state", "Confirm that the operation completed instead of silently returning to the previous screen.")

        Spacer(Modifier.height(18.dp))
        Text("BACKEND GLOBAL STATE", color = AppPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        state.systemState?.let { system ->
            SmallStatusRow("Service", if (system.online) "Online" else "Offline", system.online)
            Spacer(Modifier.height(7.dp))
            SmallStatusRow("Maintenance", if (system.maintenanceEnabled) "Enabled" else "Off", !system.maintenanceEnabled)
            Spacer(Modifier.height(7.dp))
            SmallStatusRow("Streaming feature", if (system.streamingEnabled) "Enabled" else "Disabled", system.streamingEnabled)
            Spacer(Modifier.height(7.dp))
            SmallStatusRow("Studio feature", if (system.studioEnabled) "Enabled" else "Disabled", system.studioEnabled)
        } ?: UlLoadingState("Loading global state", "Reading feature flags and maintenance state from backend.")

        Spacer(Modifier.height(18.dp))
        Text("NAVIGATION CONTRACT", color = AppPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        InfoCard("Main tabs", "Home • Studio • LIVE • Activity • Profile. Secondary features use explicit back navigation and never create duplicate bottom tabs.", AppPrimary)
        Spacer(Modifier.height(9.dp))
        InfoCard("Active Live priority", "While a broadcast is active, the app restores the Active Live workspace from notification/reopen flows instead of silently dropping the creator on Home.", AppSuccess)
        Spacer(Modifier.height(12.dp))
        UlPrimaryButton(text = "Open Functional QA", onClick = { onRoute(AppRoute.FunctionalQa) })
    }
}

// -----------------------------------------------------------------------------
// Phase 39 - End-to-End Functional QA
// -----------------------------------------------------------------------------

private data class QaCheck(val title: String, val detail: String, val pass: Boolean)

@Composable
fun Phase39FunctionalQaScreen(
    state: MobileIntegrationState,
    streamState: StreamConfigState,
    captureController: CaptureController,
    permissions: PermissionSetupController,
    onRoute: (AppRoute) -> Unit,
    onDestination: (AppDestination) -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var backendChecked by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    val config = streamState.config
    val permissionSnapshot = permissions.snapshot

    val checks = listOf(
        QaCheck("Authentication", "A valid mobile session is restored", state.isAuthenticated),
        QaCheck("Creator profile", "Profile data is available to the main app", state.profile != null),
        QaCheck("Onboarding", "Creator + permission onboarding completed", state.profile?.onboardingCompleted == true),
        QaCheck("Streaming destinations", "At least one destination exists", state.connections.isNotEmpty()),
        QaCheck("Publish-ready destination", "At least one enabled destination has credentials", state.connections.any { it.isEnabled && it.readyToPublish }),
        QaCheck("Scene workspace", "At least one backend scene exists", state.scenes.isNotEmpty()),
        QaCheck("Video configuration", "${config.resolution.label} • ${config.fps.value} FPS • ${config.bitrateKbps} Kbps", config.bitrateKbps >= 500),
        QaCheck("Microphone permission", permissionSnapshot.microphone.name, permissionSnapshot.microphone == PermissionGrantState.GRANTED || !config.microphoneEnabled),
        QaCheck("Screen capture education", "System consent is requested at Go Live time", permissionSnapshot.screenCaptureEducationComplete),
        QaCheck(
            "Backend smoke check",
            if (!backendChecked) "Not run yet" else state.finalQaRun?.let { "Server QA: ${it.status.uppercase()}" } ?: "Review Diagnostics",
            backendChecked && state.finalQaRun?.requiredPassed == true,
        ),
    )
    val passed = checks.count { it.pass }
    val progress = passed.toFloat() / checks.size.toFloat()

    PhasePage(
        title = "End-to-End Functional QA",
        subtitle = "Final mobile + backend workflow checks before release and admin integration.",
        onBack = onBack,
    ) {
        InfoCard(
            title = "$passed / ${checks.size} checks passing",
            body = if (passed == checks.size) "Current mobile prerequisites are ready for an end-to-end device test." else "Use the failed rows below to jump directly to the relevant workflow.",
            tone = if (passed == checks.size) AppSuccess else AppWarning,
            badge = if (passed == checks.size) "READY" else "REVIEW",
        )
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = if (passed == checks.size) AppSuccess else AppPrimary,
            trackColor = AppSurfaceInteractive,
        )
        Spacer(Modifier.height(16.dp))

        checks.forEach { check ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppSurface)
                    .border(1.dp, if (check.pass) AppSuccess.copy(alpha = .24f) else AppBorder, RoundedCornerShape(14.dp))
                    .padding(13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background((if (check.pass) AppSuccess else AppWarning).copy(alpha = .12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (check.pass) "✓" else "!", color = if (check.pass) AppSuccess else AppWarning, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(check.title, color = AppText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(check.detail, color = AppTextSecondary, fontSize = 10.sp, lineHeight = 14.sp)
                }
            }
            Spacer(Modifier.height(7.dp))
        }

        Spacer(Modifier.height(10.dp))
        UlPrimaryButton(
            text = "Run Backend Smoke Check",
            onClick = {
                scope.launch {
                    running = true
                    state.clearError()
                    state.refreshAccount()
                    state.refreshConnections()
                    state.refreshScenes()
                    state.refreshNotifications()
                    state.refreshHistory()
                    state.refreshSystemState()
                    state.refreshFinalDiagnostics(
                        appVersion = "0.40.0",
                        buildNumber = "39",
                        captureStatus = captureController.snapshot.status.name,
                        publishStatus = captureController.snapshot.publishStatus.name,
                    )
                    state.runFinalBackendQa()
                    backendChecked = true
                    running = false
                }
            },
            loading = running,
        )
        Spacer(Modifier.height(9.dp))
        UlActionRow("C", "Connections", "Fix or test publish destinations", onClick = { onRoute(AppRoute.Connections) })
        Spacer(Modifier.height(8.dp))
        UlActionRow("L", "Go Live Setup", "Run the complete setup and preflight path", onClick = { onRoute(AppRoute.StreamDetails) })
        Spacer(Modifier.height(8.dp))
        UlActionRow("D", "Diagnostics", "Inspect runtime values and backend state", onClick = { onRoute(AppRoute.Diagnostics) })
        Spacer(Modifier.height(8.dp))
        UlActionRow("H", "Home", "Return to the main readiness dashboard", onClick = { onDestination(AppDestination.Home) })

        Spacer(Modifier.height(14.dp))
        InfoCard(
            title = "Phase 39 scope",
            body = "This screen combines local device prerequisites with the final authenticated backend smoke run. Required backend checks are persisted for audit; destination/device checks may remain optional until a real device and live destination are configured.",
            tone = AppPrimary,
        )
    }
}
