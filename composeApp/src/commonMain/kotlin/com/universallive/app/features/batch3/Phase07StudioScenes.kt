package com.universallive.app.features.batch3

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.*
import com.universallive.app.navigation.AppDestination
import com.universallive.app.navigation.AppRoute
import com.universallive.app.streaming.overlays.SceneState
import com.universallive.app.theme.*

@Composable
fun StudioHomeV2Screen(
    sceneState: SceneState,
    onRoute: (AppRoute) -> Unit,
    onDestination: (AppDestination) -> Unit,
) {
    AppScaffold(
        title = "Studio",
        selected = AppDestination.Scenes,
        onDestinationChanged = onDestination,
    ) {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PreviewCanvas(
                label = "PREVIEW",
                footer = "${sceneState.activeScene.name} • 16:9",
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    "Mic" to "On",
                    "Audio" to "On",
                    "Face Cam" to "Off",
                    "Overlay" to "Ready",
                ).forEach { item ->
                    Surface(
                        color = AppSurface,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(
                            Modifier.padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "●",
                                color = AppPrimary,
                                fontSize = 11.sp,
                            )
                            Text(
                                item.first,
                                color = AppText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                item.second,
                                color = AppTextSecondary,
                                fontSize = 9.sp,
                            )
                        }
                    }
                }
            }

            ToolCard(
                "Current Scene",
                "${sceneState.activeScene.name} • Edit composition",
                onClick = { onRoute(AppRoute.SceneEditor) },
            )
            ToolCard(
                "Scenes",
                "${sceneState.scenes.size} scene(s) • Library and templates",
                onClick = { onRoute(AppRoute.SceneLibrary) },
            )
            ToolCard(
                "Sources",
                "Screen, camera, image, logo, text and browser",
                onClick = { onRoute(AppRoute.AddSource) },
            )
            ToolCard(
                "Audio",
                "Microphone and device/game audio",
                onClick = { onRoute(AppRoute.AudioMixer) },
            )

            UlPrimaryButton(
                "Edit Scene",
                onClick = { onRoute(AppRoute.SceneEditor) },
            )
            UlSecondaryButton(
                "Go Live",
                onClick = { onDestination(AppDestination.GoLive) },
            )
        }
    }
}

@Composable
fun SceneLibraryV2Screen(
    sceneState: SceneState,
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    StudioPage(
        title = "Scene Library",
        subtitle = "Build and switch broadcast layouts.",
        onBack = onBack,
        actions = {
            TextButton(onClick = { onRoute(AppRoute.CreateScene) }) {
                Text("+ New", color = AppPrimary)
            }
        },
    ) {
        sceneState.scenes.forEach { scene ->
            val active = scene.id == sceneState.activeSceneId
            val shape = RoundedCornerShape(18.dp)

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(if (active) AppSurfaceInteractive else AppSurface)
                    .border(
                        1.dp,
                        if (active) AppPrimary else AppBorder,
                        shape,
                    )
                    .clickable {
                        sceneState.activate(scene.id)
                        onRoute(AppRoute.SceneEditor)
                    }
                    .padding(14.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 7f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppBackgroundSecondary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("16:9", color = AppTextMuted)
                }

                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            scene.name,
                            color = AppText,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${scene.layerIds.size} layer(s)",
                            color = AppTextSecondary,
                            fontSize = 12.sp,
                        )
                    }

                    if (active) {
                        UlStatusBadge("ACTIVE", AppPrimary)
                    } else {
                        TextButton(
                            onClick = {
                                sceneState.activate(scene.id)
                            },
                        ) {
                            Text("Use", color = AppPrimary)
                        }
                    }
                }

                TextButton(
                    onClick = { onRoute(AppRoute.SceneOptions) },
                ) {
                    Text("Scene options", color = AppTextSecondary)
                }
            }
        }

        UlSecondaryButton(
            "Browse Templates",
            onClick = { onRoute(AppRoute.SceneTemplates) },
        )
    }
}

@Composable
fun CreateSceneScreen(
    sceneState: SceneState,
    onBack: () -> Unit,
    onCreated: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var choice by remember { mutableStateOf("Blank Scene") }

    StudioPage(
        title = "Create Scene",
        subtitle = "Start clean, duplicate or use a template.",
        onBack = onBack,
    ) {
        listOf(
            "Blank Scene" to "Create an empty broadcast canvas.",
            "Duplicate Current" to "Start from the active scene.",
            "Use Template" to "Choose a prepared layout.",
        ).forEach { item ->
            ToolCard(
                title = item.first,
                subtitle = item.second,
                selected = choice == item.first,
                onClick = { choice = item.first },
            )
        }

        UlTextField(
            value = name,
            onValueChange = { name = it },
            label = "Scene name",
            placeholder = "Gaming Scene",
        )

        UlPrimaryButton(
            text = "Create Scene",
            onClick = {
                sceneState.add(
                    name = name.ifBlank { "New Scene" },
                    layerIds = emptyList(),
                )
                onCreated()
            },
        )
    }
}

@Composable
fun SceneTemplatesScreen(
    onBack: () -> Unit,
) {
    StudioPage(
        title = "Scene Templates",
        subtitle = "Professional starting points for common broadcasts.",
        onBack = onBack,
    ) {
        listOf(
            "Gaming" to "Gameplay first with optional face camera.",
            "Talking" to "Large face camera and clean creator framing.",
            "Tutorial" to "Screen-first layout for demonstrations.",
            "Minimal" to "Clean full-screen composition.",
            "Starting Soon" to "Pre-broadcast waiting layout.",
            "BRB" to "Temporary away layout.",
        ).forEach { item ->
            ToolCard(
                title = item.first,
                subtitle = item.second,
                onClick = {},
            )
        }
    }
}

@Composable
fun SceneOptionsScreen(
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    StudioPage(
        title = "Scene Options",
        subtitle = "Manage the selected scene.",
        onBack = onBack,
    ) {
        ToolCard("Rename", "Change the scene name.", onClick = {})
        ToolCard("Duplicate", "Create an editable copy.", onClick = {})
        ToolCard("Set as Default", "Use this scene when Studio opens.", onClick = {})
        ToolCard("Save Preset", "Keep this layout for reuse.", onClick = {})
        ToolCard(
            "Open Editor",
            "Edit sources and composition.",
            onClick = { onRoute(AppRoute.SceneEditor) },
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AppLive, contentColor = AppText, disabledContentColor = AppText.copy(alpha = .70f)),
        ) {
            Text("Delete Scene")
        }
    }
}
