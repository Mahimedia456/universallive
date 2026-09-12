package com.universallive.app.features.batch5

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.*
import com.universallive.app.navigation.AppRoute
import com.universallive.app.theme.*

@Composable
fun PlansScreen(
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    var annual by remember { mutableStateOf(false) }

    Batch5Page(
        title = "Universal Live Plans",
        subtitle = "Choose the broadcast tools that match your workflow.",
        onBack = onBack,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !annual,
                onClick = { annual = false },
                label = { Text("Monthly") },
            )
            FilterChip(
                selected = annual,
                onClick = { annual = true },
                label = { Text("Annual") },
            )
        }

        StateCard(
            "FREE",
            "One simultaneous live destination • Up to 720p • Basic scenes and overlays.",
            status = "CURRENT",
            statusColor = AppTextMuted,
        )

        StateCard(
            "CREATOR",
            "Multistream • 1080p • Custom overlays • Advanced scenes • Enhanced audio.",
            status = "RECOMMENDED",
            statusColor = AppPrimary,
            onClick = { onRoute(AppRoute.CheckoutConfirmation) },
        )

        StateCard(
            "PRO",
            "More destinations • Advanced stream controls • Premium creator tools • Analytics.",
            onClick = { onRoute(AppRoute.CheckoutConfirmation) },
        )

        Text(
            "Prices are loaded from Apple App Store / Google Play billing. No hard-coded price is shown here.",
            color = AppTextMuted,
            fontSize = 11.sp,
        )

        UlSecondaryButton("Compare Plans", onClick = { onRoute(AppRoute.PlanComparison) })
        UlSecondaryButton("Manage Subscription", onClick = { onRoute(AppRoute.ManageSubscription) })
    }
}

@Composable
fun PlanComparisonScreen(onBack: () -> Unit) {
    Batch5Page(
        title = "Compare Plans",
        subtitle = "Mobile-friendly feature comparison.",
        onBack = onBack,
    ) {
        StateCard("Streaming", "FREE: 1 destination / 720p\nCREATOR: Multistream / 1080p\nPRO: Expanded destination limits")
        StateCard("Scenes & Overlays", "FREE: Basic\nCREATOR: Advanced creator layouts\nPRO: Premium creator tools")
        StateCard("Audio", "FREE: Core capture\nCREATOR: Enhanced controls\nPRO: Advanced production controls")
        StateCard("Analytics", "FREE: Activity history\nCREATOR: Enhanced performance\nPRO: Advanced analytics")
    }
}

@Composable
fun CheckoutConfirmationScreen(
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    var processing by remember { mutableStateOf(false) }

    Batch5Page(
        title = "Confirm Subscription",
        subtitle = "Creator plan",
        onBack = onBack,
    ) {
        StateCard("Selected Plan", "Creator • billing period selected in Plans.")
        StateCard("Price", "Store-provided price appears here when billing is connected.")
        StateCard("Renewal", "Subscription renews according to Apple App Store or Google Play terms.")

        UlPrimaryButton(
            text = "Subscribe",
            onClick = {
                processing = true
                onRoute(AppRoute.PurchaseState)
            },
            loading = processing,
        )

        Text(
            "Purchases are verified securely before membership access is updated.",
            color = AppTextMuted,
            fontSize = 11.sp,
        )
    }
}

@Composable
fun PurchaseStateScreen(
    onManage: () -> Unit,
    onBack: () -> Unit,
) {
    Batch5Page(
        title = "Subscription Updated",
        subtitle = "Purchase state",
        onBack = onBack,
    ) {
        StateCard(
            "Success",
            "Your store purchase state will be verified before premium entitlements are activated.",
            "SUCCESS",
            AppSuccess,
        )

        StateCard("Other supported states", "Processing • Failed • Pending • Restored")

        UlPrimaryButton("Manage Subscription", onClick = onManage)
    }
}

@Composable
fun ManageSubscriptionScreen(onBack: () -> Unit) {
    Batch5Page(
        title = "Manage Subscription",
        subtitle = "Current plan and store billing controls.",
        onBack = onBack,
    ) {
        StateCard("Current Plan", "Free", "ACTIVE", AppSuccess)
        StateCard("Renewal", "No paid renewal is currently configured.")
        StateCard("Billing", "Managed through the platform store when a paid plan is active.")

        UlPrimaryButton("Upgrade Plan", onClick = {})
        UlSecondaryButton("Restore Purchase", onClick = {})
        UlSecondaryButton("Manage Billing", onClick = {})

        TextButton(onClick = {}) {
            Text("Cancel Subscription", color = AppLive)
        }
    }
}
