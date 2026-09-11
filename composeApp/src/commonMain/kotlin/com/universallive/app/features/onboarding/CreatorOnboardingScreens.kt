package com.universallive.app.features.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.UlPrimaryButton
import com.universallive.app.components.UlSecondaryButton
import com.universallive.app.integration.MobileIntegrationState
import com.universallive.app.navigation.AppRoute
import com.universallive.app.theme.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import universallive.composeapp.generated.resources.Res
import universallive.composeapp.generated.resources.universallive_phase02_welcome_hero

private val OnboardingPanel = Color(0xFF08151C)
private val OnboardingPanelRaised = Color(0xFF0B1D26)
private val OnboardingBorder = Color(0xFF29414C)

@Composable
private fun CreatorOnboardingPage(
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
                    0f to Color(0xFF02090D),
                    .56f to Color(0xFF031016),
                    1f to Color(0xFF020609),
                )
            )
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                Surface(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = OnboardingPanel,
                    border = BorderStroke(1.dp, OnboardingBorder),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("‹", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Light)
                    }
                }
                Spacer(Modifier.width(12.dp))
            }
            Text(
                "$step of 6",
                color = AppTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "CREATOR SETUP",
                color = AppPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
            )
        }
        Spacer(Modifier.height(14.dp))
        OnboardingProgress(step)
        Spacer(Modifier.height(28.dp))
        Text(
            title,
            color = Color.White,
            fontSize = 29.sp,
            lineHeight = 33.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(9.dp))
        Text(
            body,
            color = AppTextSecondary,
            fontSize = 14.sp,
            lineHeight = 21.sp,
        )
        Spacer(Modifier.height(24.dp))
        content()
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun OnboardingProgress(step: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        repeat(6) { index ->
            val active = index < step
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(if (active) AppPrimary else Color(0xFF263944))
            )
        }
    }
}

@Composable
private fun SelectCard(
    title: String,
    subtitle: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        color = if (selected) Color(0xFF0C2630) else OnboardingPanel,
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) AppPrimary else OnboardingBorder,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(13.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        subtitle,
                        color = AppTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            SelectionIndicator(selected)
        }
    }
}

@Composable
private fun SelectionIndicator(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(if (selected) AppPrimary else Color.Transparent)
            .border(1.dp, if (selected) AppPrimary else Color(0xFF66818D), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Text("✓", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MonogramIcon(text: String, tint: Color = AppPrimary) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(tint.copy(alpha = .13f))
            .border(1.dp, tint.copy(alpha = .28f), RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = tint, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun OnboardingError(message: String?, onDismiss: () -> Unit) {
    if (message.isNullOrBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppLive.copy(alpha = .10f))
            .border(1.dp, AppLive.copy(alpha = .26f), RoundedCornerShape(14.dp))
            .clickable(onClick = onDismiss)
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(message, color = Color.White, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text("Dismiss", color = AppPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(14.dp))
}

/** Phase 07.1 — Welcome Creator. */
@Composable
fun CreatorSetupScreen(
    state: MobileIntegrationState,
    onNavigate: (AppRoute) -> Unit,
) {
    CreatorOnboardingPage(
        step = 1,
        title = "You're ready\nto create",
        body = "Let's personalize your streaming setup so Universal Live can prepare the right starting experience for you.",
        onBack = { onNavigate(AppRoute.AccountCreatedSuccess) },
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(330.dp),
            shape = RoundedCornerShape(24.dp),
            color = OnboardingPanel,
            border = BorderStroke(1.dp, AppPrimary.copy(alpha = .28f)),
        ) {
            Box {
                Image(
                    painter = painterResource(Res.drawable.universallive_phase02_welcome_hero),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                .50f to Color(0x22000609),
                                1f to Color(0xF202090D),
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp),
                ) {
                    Text(
                        "YOUR STUDIO. YOUR AUDIENCE.",
                        color = AppPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "Stream with confidence",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Universal Live keeps setup simple while giving you professional control when you need it.",
                        color = Color(0xFFCFDCE2),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                    )
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        UlPrimaryButton("Continue", onClick = { onNavigate(AppRoute.CreatorContentType) })
    }
}

/** Phase 07.2 — Content Type. */
@Composable
fun CreatorContentTypeScreen(
    state: MobileIntegrationState,
    onNavigate: (AppRoute) -> Unit,
) {
    val options = listOf(
        Triple("Gaming", "GM", "Gameplay and game streams"),
        Triple("IRL / Camera", "IRL", "Camera-led and real-world streams"),
        Triple("Podcast / Talk", "MIC", "Conversations, podcasts and talk"),
        Triple("Events", "EV", "Events, launches and live coverage"),
        Triple("Education", "EDU", "Teaching, tutorials and classes"),
        Triple("Music", "MU", "Performances and music sessions"),
        Triple("Other", "•••", "Something different"),
    )

    CreatorOnboardingPage(
        step = 2,
        title = "What do you stream?",
        body = "Select all that apply. We'll use this to prepare practical studio defaults.",
        onBack = { onNavigate(AppRoute.CreatorSetup) },
    ) {
        options.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowItems.forEach { (label, icon, subtitle) ->
                    val selected = label in state.onboardingContentTypes
                    Surface(
                        onClick = { state.toggleOnboardingContentType(label) },
                        modifier = Modifier.weight(1f).height(136.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = if (selected) Color(0xFF0C2630) else OnboardingPanel,
                        border = BorderStroke(
                            if (selected) 1.5.dp else 1.dp,
                            if (selected) AppPrimary else OnboardingBorder,
                        ),
                    ) {
                        Box(Modifier.padding(14.dp)) {
                            Column {
                                MonogramIcon(icon)
                                Spacer(Modifier.height(10.dp))
                                Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(3.dp))
                                Text(subtitle, color = AppTextMuted, fontSize = 9.sp, lineHeight = 12.sp)
                            }
                            Box(Modifier.align(Alignment.TopEnd)) { SelectionIndicator(selected) }
                        }
                    }
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(8.dp))
        UlPrimaryButton("Continue", onClick = { onNavigate(AppRoute.CreatorPlatforms) })
    }
}

/** Phase 07.3 — Streaming platform preferences. No credentials are requested here. */
@Composable
fun CreatorPlatformsScreen(
    state: MobileIntegrationState,
    onNavigate: (AppRoute) -> Unit,
) {
    val platforms = listOf(
        Triple("youtube", "YouTube", Color(0xFFFF3344)),
        Triple("facebook", "Facebook", Color(0xFF4385FF)),
        Triple("twitch", "Twitch", Color(0xFF9A64FF)),
        Triple("tiktok", "TikTok", AppPrimary),
        Triple("custom_rtmp", "Custom RTMP", Color(0xFFB8C7CD)),
    )

    CreatorOnboardingPage(
        step = 3,
        title = "Where do you want\nto stream?",
        body = "Choose the platforms you plan to use. This is only a preference — you'll securely connect accounts later.",
        onBack = { onNavigate(AppRoute.CreatorContentType) },
    ) {
        platforms.forEach { (key, name, color) ->
            SelectCard(
                title = name,
                subtitle = when (key) {
                    "custom_rtmp" -> "Any RTMP or RTMPS destination"
                    "youtube" -> "YouTube Live"
                    "facebook" -> "Facebook Live"
                    "twitch" -> "Twitch streaming"
                    else -> "TikTok Live"
                },
                selected = key in state.onboardingPlatforms,
                onClick = { state.toggleOnboardingPlatform(key) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                leading = {
                    MonogramIcon(
                        text = when (key) {
                            "youtube" -> "YT"
                            "facebook" -> "f"
                            "twitch" -> "TV"
                            "tiktok" -> "TT"
                            else -> "RT"
                        },
                        tint = color,
                    )
                },
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AppPrimary.copy(alpha = .07f))
                .padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("i", color = AppPrimary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(10.dp))
            Text(
                "No stream key or account credential is requested during onboarding.",
                color = AppTextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
        }
        Spacer(Modifier.height(20.dp))
        UlPrimaryButton("Continue", onClick = { onNavigate(AppRoute.CreatorExperience) })
    }
}

/** Phase 07.4 — Experience. */
@Composable
fun CreatorExperienceScreen(
    state: MobileIntegrationState,
    onNavigate: (AppRoute) -> Unit,
) {
    val options = listOf(
        Triple("new", "I'm new", "I'm just getting started with live streaming."),
        Triple("some", "I've streamed before", "I know the basics and have some experience."),
        Triple("experienced", "I'm an experienced creator", "I regularly stream and want faster access to advanced controls."),
    )

    CreatorOnboardingPage(
        step = 4,
        title = "How familiar are you\nwith live streaming?",
        body = "This helps us recommend the right starting settings without hiding professional controls.",
        onBack = { onNavigate(AppRoute.CreatorPlatforms) },
    ) {
        options.forEachIndexed { index, (key, title, subtitle) ->
            SelectCard(
                title = title,
                subtitle = subtitle,
                selected = state.onboardingExperience == key,
                onClick = { state.chooseOnboardingExperience(key) },
                modifier = Modifier.fillMaxWidth(),
                leading = { MonogramIcon((index + 1).toString()) },
            )
            Spacer(Modifier.height(11.dp))
        }
        Spacer(Modifier.height(14.dp))
        UlPrimaryButton("Continue", onClick = { onNavigate(AppRoute.CreatorGoal) })
    }
}

/** Phase 07.5 — Main goal. */
@Composable
fun CreatorGoalScreen(
    state: MobileIntegrationState,
    onNavigate: (AppRoute) -> Unit,
) {
    val goals = listOf(
        Triple("gaming", "Gaming streams", "Stream gameplay and reach viewers on your chosen platforms."),
        Triple("multiplatform", "Multi-platform streaming", "Prepare for broadcasting to multiple destinations from one workflow."),
        Triple("quality", "Better production quality", "Prioritize stable quality, scene control and polished output."),
        Triple("facecam", "Facecam + overlays", "Build a creator layout with camera and visual elements."),
        Triple("professional", "Professional broadcasts", "Prepare reliable controls for events and serious live production."),
    )

    CreatorOnboardingPage(
        step = 5,
        title = "What matters most?",
        body = "Choose your main goal. You can change your workflow later at any time.",
        onBack = { onNavigate(AppRoute.CreatorExperience) },
    ) {
        goals.forEachIndexed { index, (key, title, subtitle) ->
            SelectCard(
                title = title,
                subtitle = subtitle,
                selected = state.onboardingGoal == key,
                onClick = { state.chooseOnboardingGoal(key) },
                modifier = Modifier.fillMaxWidth(),
                leading = {
                    MonogramIcon(
                        when (index) {
                            0 -> "GM"
                            1 -> "MP"
                            2 -> "HQ"
                            3 -> "FC"
                            else -> "PRO"
                        }
                    )
                },
            )
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(12.dp))
        UlPrimaryButton("Continue", onClick = { onNavigate(AppRoute.CreatorSetupComplete) })
    }
}

/** Phase 07.6 — Setup Complete. Persists the creator setup completion through the backend. */
@Composable
fun CreatorSetupCompleteScreen(
    state: MobileIntegrationState,
    onNavigate: (AppRoute) -> Unit,
) {
    val scope = rememberCoroutineScope()

    CreatorOnboardingPage(
        step = 6,
        title = "Your streaming\nworkspace is ready",
        body = "Your creator setup is prepared. Next we'll set up the device permissions Universal Live needs for broadcasting.",
        onBack = { onNavigate(AppRoute.CreatorGoal) },
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(AppPrimary.copy(alpha = .12f))
                    .border(2.dp, AppPrimary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("✓", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(25.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = OnboardingPanel,
            border = BorderStroke(1.dp, OnboardingBorder),
        ) {
            Column(Modifier.padding(17.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Selected platforms", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Edit",
                        color = AppPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onNavigate(AppRoute.CreatorPlatforms) }
                            .padding(6.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.onboardingPlatforms.take(3).forEach { key ->
                        val label = when (key) {
                            "youtube" -> "YouTube"
                            "facebook" -> "Facebook"
                            "twitch" -> "Twitch"
                            "tiktok" -> "TikTok"
                            else -> "RTMP"
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(OnboardingPanelRaised)
                                .border(1.dp, AppPrimary.copy(alpha = .24f), RoundedCornerShape(50.dp))
                                .padding(horizontal = 11.dp, vertical = 8.dp),
                        ) {
                            Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (state.onboardingPlatforms.size > 3) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "+${state.onboardingPlatforms.size - 3} more selected",
                        color = AppTextMuted,
                        fontSize = 10.sp,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text("Content", color = AppTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.height(5.dp))
                Text(state.onboardingContentTypes.joinToString(" • "), color = Color.White, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        OnboardingError(state.error, state::clearError)
        UlPrimaryButton(
            text = "Continue to Permissions",
            onClick = {
                scope.launch {
                    if (state.completeCreatorSetup()) {
                        onNavigate(AppRoute.PermissionHub)
                    }
                }
            },
            loading = state.accountLoading,
        )
        Spacer(Modifier.height(10.dp))
        UlSecondaryButton(
            text = "Review choices",
            onClick = { onNavigate(AppRoute.CreatorContentType) },
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "LIVE BRINGS US CLOSER",
            color = AppTextMuted,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.6.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
