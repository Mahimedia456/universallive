package com.universallive.app.features.integration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.*
import com.universallive.app.features.finalpolish.ProfileActionRow
import com.universallive.app.integration.MobileIntegrationState
import com.universallive.app.navigation.AppDestination
import com.universallive.app.navigation.AppRoute
import com.universallive.app.theme.*
import kotlinx.coroutines.launch

@Composable
fun ConnectedProfileScreen(
    state: MobileIntegrationState,
    onRoute: (AppRoute) -> Unit,
    onDestinationChanged: (AppDestination) -> Unit,
) {
    LaunchedEffect(Unit) {
        state.refreshAccount()
    }

    val profile = state.profile
    val membership = state.membership

    AppScaffold(
        title = "Profile",
        selected = AppDestination.Settings,
        onDestinationChanged = onDestinationChanged,
    ) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.accountLoading && profile == null) {
                UlCard {
                    Text("Loading your account…", color = AppTextSecondary)
                }
            }

            if (!state.error.isNullOrBlank()) {
                UlCard {
                    Text(state.error ?: "", color = AppLive, fontSize = 12.sp)
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(64.dp)
                        .background(AppSurfaceRaised, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    val initials = profile?.displayName
                        ?.split(" ")
                        ?.filter { it.isNotBlank() }
                        ?.take(2)
                        ?.joinToString("") { it.first().uppercase() }
                        ?.ifBlank { "UL" }
                        ?: "UL"
                    Text(initials, color = AppPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.width(14.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        profile?.displayName?.ifBlank { "Universal Live Creator" }
                            ?: "Universal Live Creator",
                        color = AppText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "@${profile?.username?.ifBlank { "creator" } ?: "creator"}",
                        color = AppTextSecondary,
                        fontSize = 12.sp,
                    )
                    Text(
                        profile?.email?.ifBlank { state.session?.email.orEmpty() }
                            ?: state.session?.email.orEmpty(),
                        color = AppTextMuted,
                        fontSize = 11.sp,
                    )
                }

                UlStatusBadge(membership?.badge ?: "FREE", AppPrimary)
            }

            if (membership != null) {
                UlCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("${membership.planName} membership", color = AppText, fontWeight = FontWeight.Bold)
                        Text(membership.status.uppercase(), color = AppPrimary, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "${membership.maxResolution} • ${membership.maxSimultaneousDestinations} simultaneous destination${if (membership.maxSimultaneousDestinations == 1) "" else "s"}",
                        color = AppTextSecondary,
                        fontSize = 12.sp,
                    )
                    if (membership.advancedScenes) {
                        Text("Advanced scenes • overlays • analytics enabled", color = AppTextMuted, fontSize = 11.sp)
                    }
                }
            }

            ProfileActionRow(
                icon = "C",
                title = "Channel Connections",
                subtitle = "Connect, edit, test or remove YouTube, Facebook, Twitch and Custom RTMP.",
                onClick = { onRoute(AppRoute.Connections) },
            )
            ProfileActionRow(
                icon = "P",
                title = "Plans & Billing",
                subtitle = "${membership?.planName ?: "Free"} membership and entitlement controls.",
                badge = membership?.badge ?: "PLAN",
                onClick = { onRoute(AppRoute.Plans) },
            )
            ProfileActionRow(
                icon = "A",
                title = "Account & Security",
                subtitle = "Profile, email, password and sessions.",
                onClick = { onRoute(AppRoute.AccountSecurity) },
            )
            ProfileActionRow(
                icon = "S",
                title = "Streaming Defaults",
                subtitle = "Scene, destination, privacy and orientation.",
                onClick = { onRoute(AppRoute.StreamingDefaults) },
            )
            ProfileActionRow(
                icon = "V",
                title = "Video & Audio Defaults",
                subtitle = "Resolution, FPS, bitrate and microphone defaults.",
                onClick = { onRoute(AppRoute.VideoAudioDefaults) },
            )
            ProfileActionRow(
                icon = "?",
                title = "Help & Support",
                subtitle = "Guides, troubleshooting and support.",
                onClick = { onRoute(AppRoute.HelpCenter) },
            )
        }
    }
}

@Composable
fun ConnectedAccountSecurityScreen(
    state: MobileIntegrationState,
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by remember(state.profile?.displayName) {
        mutableStateOf(state.profile?.displayName.orEmpty())
    }
    var username by remember(state.profile?.username) {
        mutableStateOf(state.profile?.username.orEmpty())
    }

    com.universallive.app.features.batch6.SettingsPage(
        "Account & Security",
        "Manage the account connected to Universal Live backend.",
        onBack,
    ) {
        if (!state.error.isNullOrBlank()) {
            UlCard { Text(state.error ?: "", color = AppLive, fontSize = 12.sp) }
        }

        UlTextField(name, { name = it }, "Full name")
        UlTextField(username, { username = it }, "Username")
        UlCard {
            Text("EMAIL", color = AppTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                state.profile?.email?.ifBlank { state.session?.email.orEmpty() }
                    ?: state.session?.email.orEmpty(),
                color = AppText,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text("Email is managed by your authenticated account.", color = AppTextMuted, fontSize = 11.sp)
        }

        UlPrimaryButton(
            "Save Account",
            onClick = {
                scope.launch {
                    if (state.saveProfile(name, username)) {
                        state.refreshAccount()
                    }
                }
            },
            enabled = name.isNotBlank() && username.isNotBlank() && !state.accountLoading,
            loading = state.accountLoading,
        )

        UlSecondaryButton(
            "Sign Out",
            onClick = {
                scope.launch {
                    state.signOut()
                    onRoute(AppRoute.SignIn)
                }
            },
        )

        Spacer(Modifier.height(10.dp))
        Text("DANGER ZONE", color = AppLive, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        com.universallive.app.features.batch6.SettingCard(
            "Delete Account",
            "Permanently delete your Universal Live account and cloud data.",
            status = "DANGER",
            statusColor = AppLive,
            onClick = { onRoute(AppRoute.DeleteAccount) },
        )
    }
}
