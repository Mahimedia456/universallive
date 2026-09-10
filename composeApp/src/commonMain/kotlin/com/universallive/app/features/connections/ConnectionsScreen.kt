package com.universallive.app.features.connections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.AppScaffold
import com.universallive.app.navigation.AppDestination
import com.universallive.app.streaming.connections.RtmpProfile
import com.universallive.app.streaming.connections.RtmpProfilesState
import com.universallive.app.streaming.connections.StreamPlatform
import com.universallive.app.theme.*

@Composable
fun ConnectionsScreen(
    profilesState: RtmpProfilesState,
    onDestinationChanged: (AppDestination) -> Unit,
) {
    var selectedPlatform by remember { mutableStateOf(StreamPlatform.YouTube) }
    var profileName by remember { mutableStateOf("YouTube Main") }
    var serverUrl by remember { mutableStateOf(StreamPlatform.YouTube.defaultServerUrl) }
    var streamKey by remember { mutableStateOf("") }
    var revealKey by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    AppScaffold(
        title = "Connections",
        selected = AppDestination.Connections,
        onDestinationChanged = onDestinationChanged,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("PLATFORM", color = AppTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StreamPlatform.entries.forEach { platform ->
                    PlatformChip(
                        platform = platform,
                        selected = selectedPlatform == platform,
                        onClick = {
                            selectedPlatform = platform
                            profileName = if (platform == StreamPlatform.Custom) "Custom Server" else "${platform.label} Main"
                            serverUrl = platform.defaultServerUrl
                            validationMessage = null
                        },
                    )
                }
            }

            Text(selectedPlatform.hint, color = AppTextMuted, fontSize = 12.sp, lineHeight = 17.sp)

            ConnectionField("Profile name", profileName, { profileName = it }, false, true)
            ConnectionField("Server URL", serverUrl, { serverUrl = it }, false, false)

            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Stream key", color = AppText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(
                        if (revealKey) "HIDE" else "SHOW",
                        color = AppPrimarySoft,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { revealKey = !revealKey }.padding(6.dp),
                    )
                }
                OutlinedTextField(
                    value = streamKey,
                    onValueChange = { streamKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (revealKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    placeholder = { Text("Paste stream key", color = AppTextMuted) },
                )
            }

            validationMessage?.let {
                Text(it, color = if (it.startsWith("Saved")) AppSuccess else AppLive, fontSize = 12.sp)
            }

            Button(
                onClick = {
                    when {
                        profileName.isBlank() -> validationMessage = "Profile name is required."
                        !(serverUrl.startsWith("rtmp://") || serverUrl.startsWith("rtmps://")) -> validationMessage = "Server URL must start with rtmp:// or rtmps://"
                        streamKey.isBlank() -> validationMessage = "Stream key is required."
                        else -> {
                            profilesState.save(
                                RtmpProfile(
                                    id = 0,
                                    name = profileName.trim(),
                                    platform = selectedPlatform,
                                    serverUrl = serverUrl.trim(),
                                    streamKey = streamKey.trim(),
                                )
                            )
                            validationMessage = "Saved connection profile."
                            streamKey = ""
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("SAVE CONNECTION", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(2.dp))
            Text("SAVED CONNECTIONS", color = AppTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)

            if (profilesState.profiles.isEmpty()) {
                Text("No saved connections yet.", color = AppTextMuted, fontSize = 13.sp)
            } else {
                profilesState.profiles.forEach { profile ->
                    SavedProfileCard(profile, profilesState)
                }
            }

            Text(
                "Phase 16 adds the secure-vault architecture: Android uses Keystore-backed AES-GCM and iOS uses the same shared contract for Keychain. Multi-destination orchestration is prepared; single destination remains the safe mobile default.",
                color = AppTextMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun ConnectionField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    secret: Boolean,
    textKeyboard: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, color = AppText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (textKeyboard) KeyboardType.Text else KeyboardType.Uri),
        )
    }
}

@Composable
private fun PlatformChip(platform: StreamPlatform, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) AppSurfaceRaised else AppSurface, RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, if (selected) AppPrimary else AppBorder), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 10.dp),
    ) {
        Text(platform.label, color = if (selected) AppPrimarySoft else AppTextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SavedProfileCard(profile: RtmpProfile, profilesState: RtmpProfilesState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppSurface, RoundedCornerShape(18.dp))
            .border(BorderStroke(1.dp, AppBorder), RoundedCornerShape(18.dp))
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(profile.name, color = AppText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(profile.platform.label, color = AppPrimarySoft, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Switch(checked = profile.enabled, onCheckedChange = { profilesState.toggle(profile.id) })
        }
        Text(profile.serverUrl, color = AppTextMuted, fontSize = 11.sp, maxLines = 1)
        Text("Key: ${profile.maskedKey}", color = AppTextMuted, fontSize = 11.sp)
        TextButton(onClick = { profilesState.delete(profile.id) }, contentPadding = PaddingValues(0.dp)) {
            Text("REMOVE", color = AppLive, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
