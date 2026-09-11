package com.universallive.app.features.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.universallive.app.components.UlPrimaryButton
import com.universallive.app.components.UlSecondaryButton
import com.universallive.app.navigation.AppRoute
import com.universallive.app.navigation.VerificationFlow
import com.universallive.app.theme.*
import org.jetbrains.compose.resources.painterResource
import universallive.composeapp.generated.resources.Res
import universallive.composeapp.generated.resources.universallive_logo_on_dark

/**
 * Phase 01 — Splash.
 * The approved artwork is always shown in full. ContentScale.Fit intentionally avoids the
 * previous crop that removed top/bottom parts of the approved composition on tall phones.
 */
@Composable
fun SplashScreen() {
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.86f) }
    val taglineAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            logoAlpha.animateTo(1f, tween(durationMillis = 620, easing = FastOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(1f, tween(durationMillis = 760, easing = FastOutSlowInEasing))
        }
        delay(360)
        taglineAlpha.animateTo(1f, tween(durationMillis = 420))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 34.dp),
        ) {
            Image(
                painter = painterResource(Res.drawable.universallive_logo_on_dark),
                contentDescription = "Universal Live",
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = logoAlpha.value
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                    },
                contentScale = ContentScale.Fit,
            )
            Spacer(Modifier.height(22.dp))
            Text(
                "LIVE BRINGS US CLOSER",
                color = AppPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.2.sp,
                modifier = Modifier.graphicsLayer { alpha = taglineAlpha.value },
            )
        }
    }
}

@Composable
fun CodeExpiredScreen(flow: VerificationFlow, onNavigate: (AppRoute) -> Unit) {
    AuthScreenShell(
        onBack = { onNavigate(AppRoute.VerifyEmail(flow)) },
    ) {
        Spacer(Modifier.height(50.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AuthStatusIcon("!")
        }
        Spacer(Modifier.height(20.dp))
        AuthHeading(
            title = "Verification Code Expired",
            body = "Request a new code to continue securely. No account changes were made.",
            centered = true,
        )
        Spacer(Modifier.height(26.dp))
        UlPrimaryButton(
            text = "Request New Code",
            onClick = { onNavigate(AppRoute.VerifyEmail(flow)) },
        )
        Spacer(Modifier.height(10.dp))
        UlSecondaryButton(
            text = "Change Email",
            onClick = {
                onNavigate(
                    if (flow == VerificationFlow.AccountCreation) AppRoute.CreateAccount
                    else AppRoute.ForgotPassword
                )
            },
        )
    }
}

@Composable
fun PasswordResetSuccessScreen(onNavigate: (AppRoute) -> Unit) {
    AuthSuccessScreen(
        title = "Password Reset\nSuccessful",
        body = "You can now sign in with your new password.",
        button = "Go to Sign In",
        onClick = { onNavigate(AppRoute.SignIn) },
    )
}

@Composable
fun AccountCreatedSuccessScreen(onNavigate: (AppRoute) -> Unit) {
    AuthSuccessScreen(
        title = "Email Verified!",
        body = "Your account is now active. Let's set up your creator profile.",
        button = "Continue",
        onClick = { onNavigate(AppRoute.CreatorSetup) },
    )
}

@Composable
private fun AuthSuccessScreen(
    title: String,
    body: String,
    button: String,
    onClick: () -> Unit,
) {
    AuthScreenShell {
        Spacer(Modifier.height(92.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AuthStatusIcon("✓", success = true)
        }
        Spacer(Modifier.height(22.dp))
        Text(
            title,
            color = Color.White,
            fontSize = 28.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            body,
            color = AppTextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(30.dp))
        UlPrimaryButton(button, onClick)
        Spacer(Modifier.height(36.dp))
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
