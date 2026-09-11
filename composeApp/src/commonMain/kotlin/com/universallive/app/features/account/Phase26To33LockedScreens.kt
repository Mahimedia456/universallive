package com.universallive.app.features.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.AppScaffold
import com.universallive.app.components.UlCard
import com.universallive.app.components.UlPrimaryButton
import com.universallive.app.components.UlSecondaryButton
import com.universallive.app.components.UlStatusBadge
import com.universallive.app.components.UlTextField
import com.universallive.app.components.UniversalLiveBrand
import com.universallive.app.integration.AppNotification
import com.universallive.app.integration.MobileIntegrationState
import com.universallive.app.navigation.AppDestination
import com.universallive.app.navigation.AppRoute
import com.universallive.app.streaming.model.StreamFps
import com.universallive.app.streaming.model.StreamOrientation
import com.universallive.app.streaming.model.StreamResolution
import com.universallive.app.streaming.state.StreamConfigState
import com.universallive.app.theme.AppBackground
import com.universallive.app.theme.AppBorder
import com.universallive.app.theme.AppLive
import com.universallive.app.theme.AppPrimary
import com.universallive.app.theme.AppSuccess
import com.universallive.app.theme.AppSurface
import com.universallive.app.theme.AppSurfaceInteractive
import com.universallive.app.theme.AppSurfaceRaised
import com.universallive.app.theme.AppText
import com.universallive.app.theme.AppTextMuted
import com.universallive.app.theme.AppTextSecondary
import kotlinx.coroutines.launch

private val PhaseCorner = RoundedCornerShape(18.dp)

@Composable
private fun LockedPage(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .systemBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "‹",
                color = AppPrimary,
                fontSize = 32.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onBack)
                    .padding(horizontal = 7.dp, vertical = 1.dp),
            )
            Spacer(Modifier.width(8.dp))
            UniversalLiveBrand(compact = true)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(title, color = AppText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(subtitle, color = AppTextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))
            content()
        }
    }
}

@Composable
private fun ActionRow(
    icon: String,
    title: String,
    subtitle: String? = null,
    status: String? = null,
    statusColor: Color = AppPrimary,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PhaseCorner)
            .background(if (danger) AppLive.copy(alpha = .08f) else AppSurface)
            .border(1.dp, if (danger) AppLive.copy(alpha = .25f) else AppBorder, PhaseCorner)
            .clickable(onClick = onClick)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(if (danger) AppLive.copy(alpha = .14f) else AppSurfaceInteractive),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, color = if (danger) AppLive else AppPrimary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = if (danger) AppLive else AppText, fontWeight = FontWeight.SemiBold)
            subtitle?.let {
                Spacer(Modifier.height(3.dp))
                Text(it, color = AppTextMuted, fontSize = 11.sp)
            }
        }
        status?.let { UlStatusBadge(it, statusColor) }
        if (status == null) Text("›", color = AppTextMuted, fontSize = 24.sp)
    }
}

@Composable
private fun ChoiceRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PhaseCorner)
            .background(if (selected) AppPrimary.copy(alpha = .10f) else AppSurface)
            .border(1.dp, if (selected) AppPrimary.copy(alpha = .70f) else AppBorder, PhaseCorner)
            .clickable(onClick = onClick)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = AppText, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = AppTextMuted, fontSize = 11.sp)
        }
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) AppPrimary else AppTextMuted, CircleShape)
                .background(if (selected) AppPrimary else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Text("✓", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// -----------------------------------------------------------------------------
// Phase 26 - Notifications
// -----------------------------------------------------------------------------

@Composable
fun Phase26NotificationsScreen(
    state: MobileIntegrationState,
    onRoute: (AppRoute) -> Unit,
    onDestination: (AppDestination) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var filter by remember { mutableStateOf("All") }

    LaunchedEffect(Unit) { state.refreshNotifications() }

    val visible = state.notifications.filter {
        when (filter) {
            "Unread" -> !it.isRead
            "System" -> it.title.contains("update", true) || it.title.contains("system", true)
            else -> true
        }
    }

    AppScaffold(
        title = "Notifications",
        selected = AppDestination.Settings,
        onDestinationChanged = onDestination,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Unread", "System").forEach { label ->
                OutlinedButton(
                    onClick = { filter = label },
                    border = BorderStroke(1.dp, if (filter == label) AppPrimary else AppBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (filter == label) AppPrimary else AppSurface,
                        contentColor = if (filter == label) Color.White else AppTextSecondary,
                    ),
                    shape = RoundedCornerShape(50.dp),
                ) { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
        }
        Spacer(Modifier.height(10.dp))
        if (state.notificationUnreadCount > 0) {
            UlSecondaryButton(
                "Mark all as read (${state.notificationUnreadCount})",
                onClick = { scope.launch { state.markAllNotificationsRead() } },
            )
            Spacer(Modifier.height(10.dp))
        }

        if (state.accountLoading && state.notifications.isEmpty()) {
            UlCard { Text("Loading notifications...", color = AppTextSecondary) }
        } else if (visible.isEmpty()) {
            UlCard {
                Text("You're all caught up", color = AppText, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text("Broadcast and account updates will appear here.", color = AppTextMuted, fontSize = 12.sp)
            }
        } else {
            visible.forEach { item ->
                NotificationRow(
                    item = item,
                    onOpen = {
                        state.selectNotification(item.id)
                        onRoute(AppRoute.NotificationDetail)
                    },
                    onRead = { scope.launch { state.markNotificationRead(item.id) } },
                )
                Spacer(Modifier.height(9.dp))
            }
        }
    }
}

@Composable
private fun NotificationRow(
    item: AppNotification,
    onOpen: () -> Unit,
    onRead: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PhaseCorner)
            .background(if (item.isRead) AppSurface else AppPrimary.copy(alpha = .07f))
            .border(1.dp, if (item.isRead) AppBorder else AppPrimary.copy(alpha = .28f), PhaseCorner)
            .clickable(onClick = onOpen)
            .padding(15.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (item.isRead) AppSurfaceInteractive else AppPrimary.copy(alpha = .15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (item.isRead) "N" else "•", color = AppPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.title, color = AppText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (!item.isRead) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(AppPrimary))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(item.body, color = AppTextSecondary, fontSize = 12.sp, maxLines = 2)
            item.createdAt?.let {
                Spacer(Modifier.height(5.dp))
                Text(it.take(16).replace('T', ' '), color = AppTextMuted, fontSize = 10.sp)
            }
            if (!item.isRead) {
                TextButton(onClick = onRead) { Text("Mark as read", color = AppPrimary, fontSize = 11.sp) }
            }
        }
    }
}

@Composable
fun Phase26NotificationDetailScreen(
    state: MobileIntegrationState,
    onRoute: (AppRoute) -> Unit,
) {
    val item = state.selectedNotificationItem
    val scope = rememberCoroutineScope()

    LockedPage(
        title = "Notification Details",
        subtitle = "A closer look at this Universal Live update.",
        onBack = { onRoute(AppRoute.Notifications) },
    ) {
        if (item == null) {
            UlCard { Text("This notification is no longer available.", color = AppTextSecondary) }
            Spacer(Modifier.height(12.dp))
            UlSecondaryButton("Back to Notifications", onClick = { onRoute(AppRoute.Notifications) })
            return@LockedPage
        }

        Box(
            Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(AppSuccess.copy(alpha = .12f))
                .border(1.dp, AppSuccess.copy(alpha = .35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text("✓", color = AppSuccess, fontSize = 34.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(16.dp))
        Text(item.title, color = AppText, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(item.body, color = AppTextSecondary, fontSize = 14.sp)
        item.createdAt?.let {
            Spacer(Modifier.height(8.dp))
            Text(it.take(19).replace('T', ' '), color = AppTextMuted, fontSize = 11.sp)
        }

        Spacer(Modifier.height(20.dp))
        if (!item.isRead) {
            UlPrimaryButton("Mark as Read", onClick = { scope.launch { state.markNotificationRead(item.id) } })
            Spacer(Modifier.height(10.dp))
        }
        UlSecondaryButton("View Stream History", onClick = { onRoute(AppRoute.Main(AppDestination.Activity)) })
        Spacer(Modifier.height(9.dp))
        UlSecondaryButton("Go to Studio", onClick = { onRoute(AppRoute.Main(AppDestination.Scenes)) })
    }
}

// -----------------------------------------------------------------------------
// Phase 27 - Profile
// -----------------------------------------------------------------------------

@Composable
fun Phase27ProfileScreen(
    state: MobileIntegrationState,
    onRoute: (AppRoute) -> Unit,
    onDestination: (AppDestination) -> Unit,
) {
    LaunchedEffect(Unit) { state.refreshAccount() }
    val profile = state.profile
    val membership = state.membership
    val initials = profile?.displayName
        ?.split(" ")
        ?.filter { it.isNotBlank() }
        ?.take(2)
        ?.joinToString("") { it.take(1).uppercase() }
        ?.ifBlank { "UL" }
        ?: "UL"

    AppScaffold(
        title = "My Profile",
        selected = AppDestination.Settings,
        onDestinationChanged = onDestination,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(AppSurfaceRaised)
                    .border(2.dp, AppPrimary, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(initials, color = AppPrimary, fontSize = 27.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(12.dp))
            Text(profile?.displayName?.ifBlank { "Universal Live Creator" } ?: "Universal Live Creator", color = AppText, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text("@${profile?.username?.ifBlank { "creator" } ?: "creator"}", color = AppTextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(5.dp))
            UlStatusBadge(membership?.badge ?: "FREE", AppPrimary)
        }

        Spacer(Modifier.height(18.dp))
        UlPrimaryButton("Edit Profile", onClick = { onRoute(AppRoute.EditProfile) })
        Spacer(Modifier.height(16.dp))

        ActionRow("M", "My Membership", membership?.let { "${it.planName} • ${it.status}" } ?: "View your current plan", membership?.badge, AppPrimary) {
            onRoute(AppRoute.Plans)
        }
        Spacer(Modifier.height(9.dp))
        ActionRow("B", "Billing & Payments", "Plan status and payment management") { onRoute(AppRoute.Billing) }
        Spacer(Modifier.height(9.dp))
        ActionRow("S", "Settings", "Streaming, account, notifications and support") { onRoute(AppRoute.SettingsHub) }
        Spacer(Modifier.height(9.dp))
        ActionRow("H", "Stream History", "Review previous broadcasts and analytics") { onRoute(AppRoute.Main(AppDestination.Activity)) }
        Spacer(Modifier.height(9.dp))
        ActionRow("?", "Help & Support", "Guides, troubleshooting and support") { onRoute(AppRoute.HelpCenter) }
    }
}

// -----------------------------------------------------------------------------
// Phase 28 - Edit Profile
// -----------------------------------------------------------------------------

@Composable
fun Phase28EditProfileScreen(
    state: MobileIntegrationState,
    onRoute: (AppRoute) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var displayName by remember(state.profile?.displayName) { mutableStateOf(state.profile?.displayName.orEmpty()) }
    var username by remember(state.profile?.username) { mutableStateOf(state.profile?.username.orEmpty()) }

    LockedPage(
        title = "Edit Profile",
        subtitle = "Keep your creator identity current across Universal Live.",
        onBack = { onRoute(AppRoute.Main(AppDestination.Settings)) },
    ) {
        Box(
            Modifier
                .size(86.dp)
                .clip(CircleShape)
                .background(AppSurfaceRaised)
                .border(2.dp, AppPrimary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(displayName.take(1).ifBlank { "U" }.uppercase(), color = AppPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        Text("Profile photo sync will be connected in the backend/storage phase.", color = AppTextMuted, fontSize = 11.sp)
        Spacer(Modifier.height(18.dp))

        UlTextField(displayName, { displayName = it }, "Display name")
        Spacer(Modifier.height(10.dp))
        UlTextField(username, { username = it.removePrefix("@") }, "Username")
        Spacer(Modifier.height(10.dp))
        UlCard {
            Text("EMAIL", color = AppTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(state.profile?.email?.ifBlank { state.session?.email.orEmpty() } ?: state.session?.email.orEmpty(), color = AppText)
            Spacer(Modifier.height(4.dp))
            Text("Email changes stay in Account Settings because they require account verification.", color = AppTextMuted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(14.dp))
        if (!state.error.isNullOrBlank()) {
            Text(state.error ?: "", color = AppLive, fontSize = 12.sp)
            Spacer(Modifier.height(9.dp))
        }
        UlPrimaryButton(
            "Save Changes",
            onClick = {
                scope.launch {
                    if (state.saveProfile(displayName, username)) {
                        state.refreshAccount()
                        onRoute(AppRoute.Main(AppDestination.Settings))
                    }
                }
            },
            enabled = displayName.isNotBlank() && username.isNotBlank(),
            loading = state.accountLoading,
        )
    }
}

// -----------------------------------------------------------------------------
// Phase 29 - Membership
// -----------------------------------------------------------------------------

@Composable
fun Phase29MembershipScreen(
    state: MobileIntegrationState,
    onRoute: (AppRoute) -> Unit,
) {
    var yearly by remember { mutableStateOf(false) }
    var selectedPlan by remember(state.membership?.planKey) { mutableStateOf(state.membership?.planKey ?: "free") }
    val plans = state.billingPlans.associateBy { it.planKey }

    LockedPage(
        title = "Membership",
        subtitle = "Choose the creator tier that fits your streaming workflow.",
        onBack = { onRoute(AppRoute.Main(AppDestination.Settings)) },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50.dp))
                .background(AppSurface)
                .border(1.dp, AppBorder, RoundedCornerShape(50.dp))
                .padding(4.dp),
        ) {
            listOf(false to "Monthly", true to "Yearly").forEach { (value, label) ->
                Button(
                    onClick = { yearly = value },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (yearly == value) AppPrimary else Color.Transparent,
                        contentColor = if (yearly == value) Color.White else AppTextSecondary,
                    ),
                    shape = RoundedCornerShape(50.dp),
                ) { Text(label, fontWeight = FontWeight.Bold) }
            }
        }
        Spacer(Modifier.height(14.dp))

        MembershipPlanCard(
            title = "Free",
            price = "PKR 0",
            subtitle = "Start streaming with the essentials.",
            features = listOf("1 destination at a time", "Standard quality", "Core streaming tools"),
            selected = selectedPlan == "free",
            current = state.membership?.planKey == "free",
        ) { selectedPlan = "free" }
        Spacer(Modifier.height(10.dp))
        MembershipPlanCard(
            title = "Creator",
            price = if (yearly) plans["creator"]?.displayYearly ?: "Annual creator plan" else plans["creator"]?.displayMonthly ?: "Creator monthly",
            subtitle = "Built for serious multi-platform creators.",
            features = listOf("Multiple destinations", "1080p streaming", "Advanced scenes & overlays", "Priority tools"),
            selected = selectedPlan == "creator",
            current = state.membership?.planKey == "creator",
        ) { selectedPlan = "creator" }
        Spacer(Modifier.height(10.dp))
        MembershipPlanCard(
            title = "Pro",
            price = if (yearly) plans["pro"]?.displayYearly ?: "Annual pro plan" else plans["pro"]?.displayMonthly ?: "Pro monthly",
            subtitle = "Maximum production and analytics capability.",
            features = listOf("Highest plan limits", "Advanced analytics", "Premium production tools", "Early-access features"),
            selected = selectedPlan == "pro",
            current = state.membership?.planKey == "pro",
        ) { selectedPlan = "pro" }

        Spacer(Modifier.height(15.dp))
        if (selectedPlan == state.membership?.planKey) {
            UlCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Current plan", color = AppText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    UlStatusBadge("ACTIVE", AppSuccess)
                }
            }
        } else {
            UlPrimaryButton("Continue to Billing", onClick = { onRoute(AppRoute.Billing) })
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Plan prices and purchase confirmation will come from the mobile store/backend billing integration. This UI does not fake a purchase.",
            color = AppTextMuted,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun MembershipPlanCard(
    title: String,
    price: String,
    subtitle: String,
    features: List<String>,
    selected: Boolean,
    current: Boolean,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(PhaseCorner)
            .background(if (selected) AppPrimary.copy(alpha = .08f) else AppSurface)
            .border(1.dp, if (selected) AppPrimary else AppBorder, PhaseCorner)
            .clickable(onClick = onClick)
            .padding(17.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = AppText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(price, color = AppPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            if (current) UlStatusBadge("CURRENT", AppSuccess) else if (selected) UlStatusBadge("SELECTED", AppPrimary)
        }
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = AppTextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(9.dp))
        features.forEach { Text("✓  $it", color = AppTextSecondary, fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp)) }
    }
}

// -----------------------------------------------------------------------------
// Phase 30 - Billing
// -----------------------------------------------------------------------------

@Composable
fun Phase30BillingScreen(
    state: MobileIntegrationState,
    onRoute: (AppRoute) -> Unit,
) {
    val scope = rememberCoroutineScope()

    LockedPage(
        title = "Billing & Payments",
        subtitle = "Your plan, billing state and purchase management in one place.",
        onBack = { onRoute(AppRoute.Main(AppDestination.Settings)) },
    ) {
        UlCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)).background(AppPrimary.copy(alpha = .12f)),
                    contentAlignment = Alignment.Center,
                ) { Text("M", color = AppPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(state.membership?.planName ?: "Free", color = AppText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Status: ${state.membership?.status ?: "active"}", color = AppTextSecondary, fontSize = 11.sp)
                }
                UlStatusBadge(state.membership?.badge ?: "FREE", AppPrimary)
            }
        }
        Spacer(Modifier.height(14.dp))
        ActionRow("P", "Payment Methods", "Store-managed payment options and purchase source") { onRoute(AppRoute.PaymentMethods) }
        Spacer(Modifier.height(9.dp))
        ActionRow("M", "Manage Plan", "Compare membership options") { onRoute(AppRoute.Plans) }
        Spacer(Modifier.height(9.dp))
        ActionRow("R", "Refresh Billing Status", "Reload membership and entitlement status") {
            scope.launch { state.refreshAccount() }
        }
        Spacer(Modifier.height(14.dp))
        UlCard {
            Text("SECURE MOBILE BILLING", color = AppPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Actual charges, receipts, renewal dates and payment instruments will be supplied by the app-store purchase layer and backend verification phase.", color = AppTextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
fun Phase30PaymentMethodsScreen(
    onRoute: (AppRoute) -> Unit,
) {
    LockedPage(
        title = "Payment Methods",
        subtitle = "Universal Live will use the secure billing method attached to your mobile store account.",
        onBack = { onRoute(AppRoute.Billing) },
    ) {
        UlCard {
            Text("Store billing", color = AppText, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("No card numbers are stored in the mobile UI. Payment sources will be returned by Google Play / App Store billing once the billing integration phase is connected.", color = AppTextSecondary, fontSize = 12.sp)
        }
        Spacer(Modifier.height(14.dp))
        UlPrimaryButton("View Membership Plans", onClick = { onRoute(AppRoute.Plans) })
        Spacer(Modifier.height(9.dp))
        UlSecondaryButton("Back to Billing", onClick = { onRoute(AppRoute.Billing) })
    }
}

// -----------------------------------------------------------------------------
// Phase 31 - Settings
// -----------------------------------------------------------------------------

@Composable
fun Phase31SettingsScreen(
    state: MobileIntegrationState,
    onRoute: (AppRoute) -> Unit,
    onDestination: (AppDestination) -> Unit,
) {
    AppScaffold(
        title = "Settings",
        selected = AppDestination.Settings,
        onDestinationChanged = onDestination,
    ) {
        Text("Customize Universal Live without losing sight of the broadcast.", color = AppTextSecondary, fontSize = 12.sp)
        Spacer(Modifier.height(15.dp))
        ActionRow("A", "Account Settings", "Profile, password, session and account controls") { onRoute(AppRoute.AccountSettings) }
        Spacer(Modifier.height(9.dp))
        ActionRow("S", "Streaming Settings", "Resolution, FPS, bitrate, orientation and audio") { onRoute(AppRoute.StreamingSettings) }
        Spacer(Modifier.height(9.dp))
        ActionRow("N", "Notifications", if (state.notificationUnreadCount > 0) "${state.notificationUnreadCount} unread update(s)" else "Broadcast and account activity") { onRoute(AppRoute.Notifications) }
        Spacer(Modifier.height(9.dp))
        ActionRow("M", "Membership", state.membership?.planName ?: "Free") { onRoute(AppRoute.Plans) }
        Spacer(Modifier.height(9.dp))
        ActionRow("B", "Billing & Payments", "Purchase and entitlement status") { onRoute(AppRoute.Billing) }
        Spacer(Modifier.height(9.dp))
        ActionRow("?", "Help & Support", "Guides, troubleshooting and contact") { onRoute(AppRoute.HelpCenter) }
        Spacer(Modifier.height(9.dp))
        ActionRow("D", "Diagnostics", "Backend, capture, permissions and functional QA") { onRoute(AppRoute.Diagnostics) }
        Spacer(Modifier.height(9.dp))
        ActionRow("i", "About & Legal", "Privacy, terms and application information") { onRoute(AppRoute.LegalPrivacy) }
    }
}

// -----------------------------------------------------------------------------
// Phase 32 - Streaming Settings
// -----------------------------------------------------------------------------

@Composable
fun Phase32StreamingSettingsScreen(
    streamState: StreamConfigState,
    state: MobileIntegrationState,
    onRoute: (AppRoute) -> Unit,
) {
    val config = streamState.config
    val scope = rememberCoroutineScope()

    LockedPage(
        title = "Streaming Settings",
        subtitle = "Fine-tune the defaults Universal Live uses before every broadcast.",
        onBack = { onRoute(AppRoute.SettingsHub) },
    ) {
        Text("VIDEO SETTINGS", color = AppPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        ChoiceRow("1080p Full HD", "1920 × 1080", config.resolution == StreamResolution.P1080) { streamState.setResolution(StreamResolution.P1080) }
        Spacer(Modifier.height(8.dp))
        ChoiceRow("720p HD", "1280 × 720", config.resolution == StreamResolution.P720) { streamState.setResolution(StreamResolution.P720) }
        Spacer(Modifier.height(8.dp))
        ChoiceRow("480p", "854 × 480", config.resolution == StreamResolution.P480) { streamState.setResolution(StreamResolution.P480) }

        Spacer(Modifier.height(16.dp))
        Text("FRAME RATE", color = AppPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        ChoiceRow("30 FPS", "Stable default for mobile streaming", config.fps == StreamFps.FPS30) { streamState.setFps(StreamFps.FPS30) }
        Spacer(Modifier.height(8.dp))
        ChoiceRow("60 FPS", "Higher motion clarity; requires more device/network headroom", config.fps == StreamFps.FPS60) { streamState.setFps(StreamFps.FPS60) }

        Spacer(Modifier.height(16.dp))
        Text("BITRATE", color = AppPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        listOf(4500, 6000, 6800, 8000).forEach { value ->
            ChoiceRow(
                title = "$value Kbps",
                subtitle = if (value == 6800) "Recommended 1080p default" else "Manual video bitrate",
                selected = config.bitrateKbps == value,
                onClick = { streamState.setBitrateKbps(value) },
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(8.dp))
        Text("ORIENTATION", color = AppPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        StreamOrientation.values().forEach { orientation ->
            ChoiceRow(orientation.label, "Stream canvas orientation", config.orientation == orientation) { streamState.setOrientation(orientation) }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(8.dp))
        SettingToggle("Microphone", "Include microphone by default", config.microphoneEnabled) { streamState.setMicrophoneEnabled(it) }
        Spacer(Modifier.height(9.dp))
        SettingToggle("Game / Device Audio", "Include internal audio by default", config.internalAudioEnabled) { streamState.setInternalAudioEnabled(it) }

        Spacer(Modifier.height(16.dp))
        UlPrimaryButton(
            "Save Settings",
            onClick = {
                scope.launch {
                    val payload = """{"width":${config.resolution.width},"height":${config.resolution.height},"fps":${config.fps.value},"bitrateKbps":${config.bitrateKbps},"orientation":"${config.orientation.name.lowercase()}","microphoneEnabled":${config.microphoneEnabled},"internalAudioEnabled":${config.internalAudioEnabled}}"""
                    if (state.saveStreamingDefaults(payload)) onRoute(AppRoute.SettingsHub)
                }
            },
            loading = state.accountLoading,
        )
        Spacer(Modifier.height(9.dp))
        UlSecondaryButton("Reset to Recommended", onClick = { streamState.reset() })
    }
}

@Composable
private fun SettingToggle(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(PhaseCorner)
            .background(AppSurface)
            .border(1.dp, AppBorder, PhaseCorner)
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = AppText, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = AppTextMuted, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AppPrimary,
                uncheckedThumbColor = AppTextSecondary,
                uncheckedTrackColor = AppSurfaceInteractive,
            ),
        )
    }
}

// -----------------------------------------------------------------------------
// Phase 33 - Account Settings
// -----------------------------------------------------------------------------

@Composable
fun Phase33AccountSettingsScreen(
    state: MobileIntegrationState,
    onRoute: (AppRoute) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var showPassword by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    LockedPage(
        title = "Account Settings",
        subtitle = "Your Universal Live account, security and session controls.",
        onBack = { onRoute(AppRoute.SettingsHub) },
    ) {
        UlCard {
            Text("ACCOUNT", color = AppPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(state.profile?.email?.ifBlank { state.session?.email.orEmpty() } ?: state.session?.email.orEmpty(), color = AppText, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text("@${state.profile?.username?.ifBlank { "creator" } ?: "creator"}", color = AppTextMuted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(12.dp))
        ActionRow("P", "Edit Profile", "Display name and creator username") { onRoute(AppRoute.EditProfile) }
        Spacer(Modifier.height(9.dp))
        ActionRow("K", "Change Password", "Update the password for this authenticated account") { showPassword = !showPassword }

        if (showPassword) {
            Spacer(Modifier.height(10.dp))
            UlCard {
                UlTextField(newPassword, { newPassword = it }, "New password", visualTransformation = PasswordVisualTransformation())
                Spacer(Modifier.height(9.dp))
                UlTextField(confirmPassword, { confirmPassword = it }, "Confirm password", visualTransformation = PasswordVisualTransformation())
                Spacer(Modifier.height(10.dp))
                UlPrimaryButton(
                    "Update Password",
                    onClick = {
                        scope.launch {
                            if (state.updatePassword(newPassword)) {
                                newPassword = ""
                                confirmPassword = ""
                                showPassword = false
                            }
                        }
                    },
                    enabled = newPassword.length >= 8 && newPassword == confirmPassword,
                    loading = state.loading,
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Text("SESSION", color = AppPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("${state.accountSessions.count { it.active }} active signed-in session(s)", color = AppTextMuted, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        UlSecondaryButton("Sign Out All Devices", onClick = {
            scope.launch {
                if (state.logoutAllSessions()) onRoute(AppRoute.SignIn)
            }
        })
        Spacer(Modifier.height(8.dp))
        UlSecondaryButton("Sign Out", onClick = {
            scope.launch {
                state.signOut()
                onRoute(AppRoute.SignIn)
            }
        })

        Spacer(Modifier.height(20.dp))
        Text("DANGER ZONE", color = AppLive, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        ActionRow("!", "Delete Account", "Permanently delete the account and backend-synced data", danger = true) {
            onRoute(AppRoute.DeleteAccount)
        }
    }
}
