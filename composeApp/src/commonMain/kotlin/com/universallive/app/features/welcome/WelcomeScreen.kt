package com.universallive.app.features.welcome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.UlPrimaryButton
import com.universallive.app.components.UlSecondaryButton
import com.universallive.app.components.UniversalLiveBrand
import com.universallive.app.navigation.AppRoute
import com.universallive.app.theme.*
import org.jetbrains.compose.resources.painterResource
import universallive.composeapp.generated.resources.Res
import universallive.composeapp.generated.resources.universallive_phase02_welcome_hero

/**
 * Locked Phase 02 welcome / intro experience.
 *
 * Three focused pages mirror the approved authentication design board:
 *  1. Welcome to Universal Live
 *  2. More Than Just Streaming
 *  3. Stream Everywhere
 *
 * All calls-to-action are live routes; no decorative dead buttons are used.
 */
@Composable
fun WelcomeScreen(onNavigate: (AppRoute) -> Unit) {
    var page by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground),
    ) {
        Image(
            painter = painterResource(Res.drawable.universallive_phase02_welcome_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xB5000509),
                            Color(0x9A01070B),
                            Color(0xEE020609),
                            AppBackground,
                        ),
                        startY = 0f,
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 22.dp),
        ) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                UniversalLiveBrand(compact = true)
                if (page > 0) {
                    Text(
                        "Skip",
                        color = AppPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onNavigate(AppRoute.CreateAccount) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }

            Spacer(Modifier.weight(.42f))

            when (page) {
                0 -> WelcomeIntroPage()
                1 -> CommunityIntroPage()
                else -> PlatformsIntroPage()
            }

            Spacer(Modifier.height(18.dp))
            IntroDots(page = page, count = 3)
            Spacer(Modifier.height(18.dp))

            if (page < 2) {
                UlPrimaryButton(
                    text = if (page == 0) "Get Started" else "Continue",
                    onClick = { page += 1 },
                )
            } else {
                UlPrimaryButton(
                    text = "Create Account",
                    onClick = { onNavigate(AppRoute.CreateAccount) },
                )
            }
            Spacer(Modifier.height(10.dp))
            UlSecondaryButton(
                text = "Sign In",
                onClick = { onNavigate(AppRoute.SignIn) },
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = when (page) {
                    0 -> "A BIGGER AUDIENCE AWAITS"
                    1 -> "PEOPLE  •  STORIES  •  COMMUNITIES"
                    else -> "CONNECT TO THE WORLD"
                },
                color = AppTextMuted,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun WelcomeIntroPage() {
    Column {
        Text(
            "STREAM  •  CREATE  •  CONNECT  ANYWHERE",
            color = AppTextSecondary,
            fontSize = 9.sp,
            letterSpacing = 1.45.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                    append("Welcome to\n")
                }
                withStyle(SpanStyle(color = AppPrimary, fontWeight = FontWeight.Bold)) {
                    append("Universal Live")
                }
            },
            fontSize = 34.sp,
            lineHeight = 37.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Stream. Create. Connect. Anywhere.",
            color = AppTextSecondary,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(18.dp))
        FeatureLine("◎", "Go live on multiple platforms")
        Spacer(Modifier.height(10.dp))
        FeatureLine("◇", "Professional streaming tools")
        Spacer(Modifier.height(10.dp))
        FeatureLine("◉", "Join a global creator community")
    }
}

@Composable
private fun CommunityIntroPage() {
    Column {
        Text(
            "TURN YOUR MOMENTS INTO A GLOBAL STORY",
            color = AppPrimary,
            fontSize = 10.sp,
            letterSpacing = 1.65.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                    append("More Than\n")
                }
                withStyle(SpanStyle(color = AppPrimary, fontWeight = FontWeight.Bold)) {
                    append("Just Streaming")
                }
            },
            fontSize = 34.sp,
            lineHeight = 37.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Games, events, creativity and real conversations. Universal Live gives you the tools to share what you love with the world.",
            color = AppTextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntroPill("GAMES", Modifier.weight(1f))
            IntroPill("LIVE EVENTS", Modifier.weight(1f))
            IntroPill("CREATIVE", Modifier.weight(1f))
        }
    }
}

@Composable
private fun PlatformsIntroPage() {
    Column {
        Text(
            "ONE APP. MANY PLATFORMS.",
            color = AppPrimary,
            fontSize = 10.sp,
            letterSpacing = 1.65.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                    append("Stream\n")
                }
                withStyle(SpanStyle(color = AppPrimary, fontWeight = FontWeight.Bold)) {
                    append("Everywhere")
                }
            },
            fontSize = 34.sp,
            lineHeight = 37.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Go live on YouTube, Facebook, Twitch, TikTok and custom RTMP — all from one app.",
            color = AppTextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PlatformTile("▶", "YouTube", Modifier.weight(1f))
            PlatformTile("f", "Facebook", Modifier.weight(1f))
            PlatformTile("▣", "Twitch", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PlatformTile("♪", "TikTok", Modifier.weight(1f))
            PlatformTile("◉", "RTMP", Modifier.weight(1f))
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun FeatureLine(symbol: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(AppPrimary.copy(alpha = .08f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(symbol, color = AppPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun IntroPill(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color(0xC508151C),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AppPrimary.copy(alpha = .38f)),
    ) {
        Text(
            text,
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        )
    }
}

@Composable
private fun PlatformTile(symbol: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color(0xDC07131A),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AppPrimary.copy(alpha = .23f)),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
        ) {
            Text(symbol, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(label, color = AppTextSecondary, fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
private fun IntroDots(page: Int, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .width(if (index == page) 18.dp else 6.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(if (index == page) AppPrimary else AppTextMuted.copy(alpha = .55f)),
            )
        }
    }
}
