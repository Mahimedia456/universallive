package com.universallive.app.features.batch6

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.universallive.app.components.*
import com.universallive.app.navigation.AppRoute
import com.universallive.app.theme.*

@Composable
fun OfflineScreen(onRetry: () -> Unit, onHome: () -> Unit) {
    SettingsPage("You're Offline", "Universal Live cannot reach backend services.") {
        SettingCard(
            "No internet connection",
            "Local scene editing and some saved settings may remain available. Live destinations and account sync require internet.",
            "OFFLINE",
            AppWarning,
        )
        UlPrimaryButton("Retry Connection", onClick = onRetry)
        UlSecondaryButton("Go Home", onClick = onHome)
    }
}

@Composable
fun SessionExpiredScreen(onSignIn: () -> Unit) {
    SettingsPage("Session Expired", "Sign in again to continue securely.") {
        SettingCard(
            "Your session ended",
            "Universal Live will preserve supported local unsaved state while you authenticate again.",
            "AUTH",
            AppWarning,
        )
        UlPrimaryButton("Sign In Again", onClick = onSignIn)
    }
}

@Composable
fun PermissionBlockedScreen(onBack: () -> Unit) {
    SettingsPage("Permission Required", "A required device capability is currently blocked.", onBack) {
        SettingCard("Microphone blocked", "Open system settings and allow microphone access when you want voice capture.", "BLOCKED", AppWarning)
        SettingCard("Camera blocked", "Allow camera access before using face camera.", "BLOCKED", AppWarning)
        SettingCard("Screen capture unavailable", "Screen capture permission is requested by the operating system when starting a broadcast.", "CHECK", AppWarning)
        UlPrimaryButton("Open Settings", onClick = {})
        UlSecondaryButton("Not Now", onClick = onBack)
    }
}

@Composable
fun ServiceErrorScreen(onRetry: () -> Unit, onHome: () -> Unit) {
    SettingsPage("Service Unavailable", "Universal Live could not complete the request.") {
        SettingCard(
            "Temporary service problem",
            "Retry the request. If the problem continues, use Support from Profile.",
            "ERROR",
            AppLive,
        )
        UlPrimaryButton("Retry", onClick = onRetry)
        UlSecondaryButton("Go Home", onClick = onHome)
    }
}

@Composable
fun DesignSystemStatesScreen(onBack: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var toggle by remember { mutableStateOf(true) }
    var checked by remember { mutableStateOf(true) }
    var slider by remember { mutableStateOf(.62f) }

    SettingsPage("UI System Audit", "Production examples for shared Universal Live components.", onBack) {
        SettingCard("Buttons") {
            UlPrimaryButton("Primary Action", onClick = {})
            Spacer(Modifier.height(8.dp))
            UlSecondaryButton("Secondary Action", onClick = {})
            Spacer(Modifier.height(8.dp))
            UlPrimaryButton("Disabled", onClick = {}, enabled = false)
        }

        SettingCard("Inputs") {
            UlTextField(input, { input = it }, "Text input")
            Spacer(Modifier.height(8.dp))
            UlTextField(
                password,
                { password = it },
                "Password",
                visualTransformation = PasswordVisualTransformation(),
            )
        }

        SettingCard("Selection") {
            Row(Modifier.fillMaxWidth()) {
                Checkbox(checked, { checked = it })
                Text("Checkbox", color = AppText, modifier = Modifier.padding(top = 12.dp))
            }
            ToggleSetting("Switch", "Boolean setting", toggle) { toggle = it }
            Slider(
                value = slider,
                onValueChange = { slider = it },
                colors = SliderDefaults.colors(
                    thumbColor = AppPrimary,
                    activeTrackColor = AppPrimary,
                ),
            )
        }

        SettingCard("Status") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UlStatusBadge("READY", AppSuccess)
                UlStatusBadge("LIVE", AppLive)
                UlStatusBadge("CONNECTING", AppPrimary)
            }
        }

        SettingCard("Production states", "Loading • skeleton • empty • success • error • disabled • snackbar • bottom sheet • confirmation dialog.")
        SettingCard("Final audit", "Safe areas • keyboard handling • touch targets • contrast • scroll behavior • landscape readiness • Android/iOS consistency.", "QA", AppPrimary)
    }
}
