package com.universallive.app.features.batch3

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.*
import com.universallive.app.theme.*

@Composable
fun FacecamEditorScreen(onBack: () -> Unit) {
    var front by remember { mutableStateOf(true) }
    var mirror by remember { mutableStateOf(true) }
    var size by remember { mutableStateOf(.26f) }
    var opacity by remember { mutableStateOf(1f) }
    var shape by remember { mutableStateOf("Circle") }

    StudioPage(
        title = "Face Camera",
        subtitle = "Position and style your live camera.",
        onBack = onBack,
    ) {
        PreviewCanvas("FACE CAM", "Live camera preview")

        EditorSection("Camera") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = front,
                    onClick = { front = true },
                    label = { Text("Front") },
                )
                FilterChip(
                    selected = !front,
                    onClick = { front = false },
                    label = { Text("Rear") },
                )
            }

            Row(Modifier.fillMaxWidth()) {
                Text("Mirror", color = AppText, modifier = Modifier.weight(1f))
                Switch(mirror, { mirror = it })
            }
        }

        EditorSection("Shape") {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf("Circle", "Rounded", "Rectangle").forEach {
                    FilterChip(
                        selected = shape == it,
                        onClick = { shape = it },
                        label = { Text(it, fontSize = 11.sp) },
                    )
                }
            }
        }

        EditorSection("Size") {
            Slider(
                size,
                { size = it },
                valueRange = .12f..0.5f,
                colors = SliderDefaults.colors(
                    thumbColor = AppPrimary,
                    activeTrackColor = AppPrimary,
                ),
            )
            LabelValue("Size", "${(size * 100).toInt()}%")
        }

        EditorSection("Appearance") {
            Slider(
                opacity,
                { opacity = it },
                colors = SliderDefaults.colors(
                    thumbColor = AppPrimary,
                    activeTrackColor = AppPrimary,
                ),
            )
            LabelValue("Opacity", "${(opacity * 100).toInt()}%")
            LabelValue("Border", "Cyan • 1 px")
            LabelValue("Shadow", "Subtle")
        }

        UlPrimaryButton("Apply Face Camera", onClick = onBack)
    }
}

@Composable
fun ImageLogoEditorScreen(onBack: () -> Unit) {
    var opacity by remember { mutableStateOf(1f) }
    var fit by remember { mutableStateOf("Fit") }

    StudioPage(
        title = "Image / Logo",
        subtitle = "Place a creator-owned visual asset.",
        onBack = onBack,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 7f)
                .background(AppSurface, RoundedCornerShape(18.dp))
                .border(1.dp, AppBorder, RoundedCornerShape(18.dp)),
        ) {
            Text(
                "Asset preview",
                color = AppTextMuted,
                modifier = Modifier.padding(18.dp),
            )
        }

        UlSecondaryButton("Replace Asset", onClick = {})

        EditorSection("Fit") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Fit", "Fill", "Crop").forEach {
                    FilterChip(
                        selected = fit == it,
                        onClick = { fit = it },
                        label = { Text(it) },
                    )
                }
            }
        }

        EditorSection("Appearance") {
            Slider(
                opacity,
                { opacity = it },
                colors = SliderDefaults.colors(
                    thumbColor = AppPrimary,
                    activeTrackColor = AppPrimary,
                ),
            )
            LabelValue("Opacity", "${(opacity * 100).toInt()}%")
            LabelValue("Corner radius", "12")
            LabelValue("Border", "None")
            LabelValue("Shadow", "Subtle")
        }

        UlPrimaryButton("Apply", onClick = onBack)
    }
}

@Composable
fun TextEditorScreen(onBack: () -> Unit) {
    var text by remember { mutableStateOf("My live stream") }
    var fontSize by remember { mutableStateOf(.45f) }
    var background by remember { mutableStateOf(false) }

    StudioPage(
        title = "Text",
        subtitle = "Add clean broadcast typography.",
        onBack = onBack,
    ) {
        UlTextField(
            value = text,
            onValueChange = { text = it },
            label = "Text",
        )

        EditorSection("Typography") {
            LabelValue("Font", "System Sans")
            LabelValue("Weight", "Semi Bold")
            Slider(
                fontSize,
                { fontSize = it },
                colors = SliderDefaults.colors(
                    thumbColor = AppPrimary,
                    activeTrackColor = AppPrimary,
                ),
            )
            LabelValue("Size", "${24 + (fontSize * 48).toInt()} sp")
            LabelValue("Alignment", "Left")
            LabelValue("Text color", "White")
        }

        EditorSection("Background") {
            Row(Modifier.fillMaxWidth()) {
                Text("Text background", color = AppText, modifier = Modifier.weight(1f))
                Switch(background, { background = it })
            }
            LabelValue("Opacity", if (background) "72%" else "Off")
            LabelValue("Shadow", "Subtle")
        }

        UlPrimaryButton("Apply Text", onClick = onBack)
    }
}

@Composable
fun BrowserSourceEditorScreen(onBack: () -> Unit) {
    var name by remember { mutableStateOf("Browser Overlay") }
    var url by remember { mutableStateOf("") }
    var interaction by remember { mutableStateOf(false) }

    StudioPage(
        title = "Browser Source",
        subtitle = "Add a trusted web-based broadcast source.",
        onBack = onBack,
    ) {
        UlTextField(
            value = name,
            onValueChange = { name = it },
            label = "Name",
        )
        UlTextField(
            value = url,
            onValueChange = { url = it },
            label = "URL",
            placeholder = "https://...",
        )

        EditorSection("Viewport") {
            LabelValue("Width", "1920")
            LabelValue("Height", "1080")
        }

        EditorSection("Interaction") {
            Row(Modifier.fillMaxWidth()) {
                Text(
                    "Enable interaction",
                    color = AppText,
                    modifier = Modifier.weight(1f),
                )
                Switch(interaction, { interaction = it })
            }
            Text(
                "Only enable interaction for a source you trust.",
                color = AppTextMuted,
                fontSize = 11.sp,
            )
        }

        UlSecondaryButton("Refresh Source", onClick = {})
        UlPrimaryButton("Add Browser Source", onClick = onBack)
    }
}

@Composable
fun BackgroundEditorScreen(onBack: () -> Unit) {
    var type by remember { mutableStateOf("Transparent") }

    StudioPage(
        title = "Background",
        subtitle = "Set the base layer behind your scene.",
        onBack = onBack,
    ) {
        listOf(
            "Transparent" to "No background layer.",
            "Solid" to "Single brand-safe color.",
            "Gradient" to "Subtle approved dark/cyan gradient.",
            "Image" to "Use a creator-owned image.",
        ).forEach { item ->
            ToolCard(
                title = item.first,
                subtitle = item.second,
                selected = type == item.first,
                onClick = { type = item.first },
            )
        }

        if (type == "Solid") {
            EditorSection("Solid Color") {
                LabelValue("Color", "#020609")
            }
        }

        if (type == "Gradient") {
            EditorSection("Gradient") {
                LabelValue("Start", "#020609")
                LabelValue("End", "#087F99")
                Text(
                    "Use restrained cyan only; avoid random colorful gradients.",
                    color = AppTextMuted,
                    fontSize = 11.sp,
                )
            }
        }

        if (type == "Image") {
            UlSecondaryButton("Choose Image", onClick = {})
        }

        UlPrimaryButton("Apply Background", onClick = onBack)
    }
}
