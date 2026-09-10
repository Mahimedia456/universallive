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
import com.universallive.app.navigation.AppRoute
import com.universallive.app.streaming.overlays.SceneState
import com.universallive.app.theme.*

@Composable
fun SceneEditorV2Screen(
    sceneState: SceneState,
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    StudioPage(
        title = sceneState.activeScene.name,
        subtitle = "Scene Editor",
        onBack = onBack,
        actions = {
            TextButton(onClick = {}) { Text("Undo", color = AppTextSecondary) }
            TextButton(onClick = {}) { Text("Save", color = AppPrimary) }
        },
    ) {
        Box {
            PreviewCanvas(
                label = "EDITING",
                footer = "Drag • Resize • Position",
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(.38f)
                    .aspectRatio(1.45f)
                    .border(
                        1.dp,
                        AppPrimary,
                        RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Selected Source",
                    color = AppPrimary,
                    fontSize = 11.sp,
                )
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            listOf(
                "Sources" to AppRoute.AddSource,
                "Layers" to AppRoute.Layers,
                "Transform" to AppRoute.TransformInspector,
                "Properties" to AppRoute.SourceProperties,
            ).forEach { item ->
                OutlinedButton(
                    onClick = { onRoute(item.second) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    Text(
                        item.first,
                        fontSize = 10.sp,
                        color = AppText,
                    )
                }
            }
        }

        EditorSection("Composition") {
            LabelValue("Active scene", sceneState.activeScene.name)
            LabelValue("Canvas", "16:9")
            LabelValue("Output", "1080p")
        }

        UlPrimaryButton(
            "Add Source",
            onClick = { onRoute(AppRoute.AddSource) },
        )
    }
}

@Composable
fun AddSourceScreen(
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    StudioPage(
        title = "Add Source",
        subtitle = "Choose what appears in this scene.",
        onBack = onBack,
    ) {
        Text(
            "CAPTURE",
            color = AppPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        ToolCard(
            "Screen / Gameplay",
            "Entire-device capture source.",
            onClick = {},
        )
        ToolCard(
            "Camera",
            "Front or rear camera source.",
            onClick = { onRoute(AppRoute.FacecamEditor) },
        )

        Spacer(Modifier.height(3.dp))
        Text(
            "VISUAL",
            color = AppPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        ToolCard(
            "Image / Logo",
            "Add an image or creator logo.",
            onClick = { onRoute(AppRoute.ImageLogoEditor) },
        )
        ToolCard(
            "Text",
            "Add titles, labels and creator text.",
            onClick = { onRoute(AppRoute.TextEditor) },
        )
        ToolCard(
            "Background",
            "Solid, gradient or image background.",
            onClick = { onRoute(AppRoute.BackgroundEditor) },
        )

        Spacer(Modifier.height(3.dp))
        Text(
            "INTERACTIVE",
            color = AppPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        ToolCard("Chat", "Live chat overlay.", onClick = { onRoute(AppRoute.ChatOverlayEditor) })
        ToolCard("Alerts", "Creator event alerts.", onClick = { onRoute(AppRoute.AlertEditor) })
        ToolCard("Goal", "Progress and creator goal overlay.", onClick = { onRoute(AppRoute.GoalOverlayEditor) })
        ToolCard(
            "Browser Source",
            "Secure web-based overlay URL.",
            onClick = { onRoute(AppRoute.BrowserSourceEditor) },
        )
    }
}

@Composable
fun LayersScreen(
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    val layers = remember {
        mutableStateListOf(
            "Face Cam",
            "Creator Logo",
            "Chat",
            "Alerts",
            "Screen Capture",
        )
    }

    StudioPage(
        title = "Layers",
        subtitle = "Top items appear above lower items.",
        onBack = onBack,
    ) {
        layers.forEachIndexed { index, item ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(15.dp))
                    .background(AppSurface)
                    .border(1.dp, AppBorder, RoundedCornerShape(15.dp))
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "≡",
                        color = AppTextMuted,
                        fontSize = 20.sp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            item,
                            color = AppText,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Layer ${index + 1}",
                            color = AppTextMuted,
                            fontSize = 11.sp,
                        )
                    }
                    Text("◉", color = AppPrimary)
                    Spacer(Modifier.width(12.dp))
                    Text("🔒", color = AppTextMuted)
                }
                TextButton(
                    onClick = { onRoute(AppRoute.SourceProperties) },
                ) {
                    Text(
                        "Properties",
                        color = AppPrimary,
                    )
                }
            }
        }
    }
}

@Composable
fun TransformInspectorScreen(
    onBack: () -> Unit,
) {
    var size by remember { mutableStateOf(.62f) }
    var rotation by remember { mutableStateOf(0f) }

    StudioPage(
        title = "Transform",
        subtitle = "Position and size the selected source.",
        onBack = onBack,
    ) {
        PreviewCanvas(
            label = "TRANSFORM",
            footer = "Selected source",
        )

        EditorSection("Position") {
            LabelValue("X", "50%")
            LabelValue("Y", "50%")
        }

        EditorSection("Size") {
            Slider(
                value = size,
                onValueChange = { size = it },
                colors = SliderDefaults.colors(
                    thumbColor = AppPrimary,
                    activeTrackColor = AppPrimary,
                ),
            )
            LabelValue("Scale", "${(size * 100).toInt()}%")
        }

        EditorSection("Rotation") {
            Slider(
                value = rotation,
                onValueChange = { rotation = it },
                valueRange = -180f..180f,
                colors = SliderDefaults.colors(
                    thumbColor = AppPrimary,
                    activeTrackColor = AppPrimary,
                ),
            )
            LabelValue("Rotation", "${rotation.toInt()}°")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) {
                Text("Fit")
            }
            OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) {
                Text("Fill")
            }
            OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) {
                Text("Reset")
            }
        }
    }
}

@Composable
fun SourcePropertiesScreen(
    onBack: () -> Unit,
) {
    var opacity by remember { mutableStateOf(1f) }
    var visible by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }

    StudioPage(
        title = "Source Properties",
        subtitle = "Selected source controls.",
        onBack = onBack,
    ) {
        EditorSection("Appearance") {
            Text("Opacity", color = AppTextSecondary)
            Slider(
                value = opacity,
                onValueChange = { opacity = it },
                colors = SliderDefaults.colors(
                    thumbColor = AppPrimary,
                    activeTrackColor = AppPrimary,
                ),
            )
            LabelValue("Opacity", "${(opacity * 100).toInt()}%")
        }

        EditorSection("Source") {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Visible", color = AppText, modifier = Modifier.weight(1f))
                Switch(visible, { visible = it })
            }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Lock", color = AppText, modifier = Modifier.weight(1f))
                Switch(locked, { locked = it })
            }
        }

        UlSecondaryButton("Duplicate Source", onClick = {})

        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AppLive, contentColor = AppText, disabledContentColor = AppText.copy(alpha = .70f)),
        ) {
            Text("Delete Source")
        }
    }
}
