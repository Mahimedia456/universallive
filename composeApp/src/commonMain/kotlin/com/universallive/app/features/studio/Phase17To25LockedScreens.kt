package com.universallive.app.features.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universallive.app.components.UlPrimaryButton
import com.universallive.app.components.UlSecondaryButton
import com.universallive.app.components.UlTextField
import com.universallive.app.components.UniversalLiveBrand
import com.universallive.app.integration.MobileIntegrationState
import com.universallive.app.integration.StreamHistoryItem
import com.universallive.app.navigation.AppDestination
import com.universallive.app.navigation.AppRoute
import com.universallive.app.streaming.capture.CaptureController
import com.universallive.app.streaming.facecam.FacecamLens
import com.universallive.app.streaming.facecam.FacecamShape
import com.universallive.app.streaming.facecam.FacecamState
import com.universallive.app.streaming.model.StreamFps
import com.universallive.app.streaming.model.StreamOrientation
import com.universallive.app.streaming.model.StreamResolution
import com.universallive.app.streaming.overlays.OverlayKind
import com.universallive.app.streaming.overlays.OverlayLayer
import com.universallive.app.streaming.overlays.OverlayState
import com.universallive.app.streaming.overlays.SceneState
import com.universallive.app.streaming.state.StreamConfigState
import com.universallive.app.theme.*
import kotlinx.coroutines.launch

private val StudioPanel = Color(0xFF071116)
private val StudioPanelRaised = Color(0xFF0B1820)
private val Purple = Color(0xFF8B5CF6)
private val Pink = Color(0xFFFF3D8D)
private val Blue = Color(0xFF4E8CFF)
private val Orange = Color(0xFFFF9F43)

@Composable
private fun LockedStudioPage(
    phase: String,
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(AppBackground).systemBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                Surface(
                    onClick = onBack,
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(13.dp),
                    color = StudioPanel,
                    border = BorderStroke(1.dp, AppBorder),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("‹", color = AppText, fontSize = 28.sp)
                    }
                }
                Spacer(Modifier.width(10.dp))
            }
            UniversalLiveBrand(compact = true, showTagline = false)
        }

        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            Text(phase.uppercase(), color = AppPrimary, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.8.sp)
            Spacer(Modifier.height(8.dp))
            Text(title, color = AppText, fontSize = 29.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text(subtitle, color = AppTextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(20.dp))
            content()
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun StudioCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.fillMaxWidth().clip(shape)
            .background(if (selected) AppPrimary.copy(alpha = .07f) else StudioPanel)
            .border(1.dp, if (selected) AppPrimary.copy(alpha = .70f) else AppBorder, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun ToolCard(
    icon: String,
    title: String,
    subtitle: String,
    color: Color = AppPrimary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StudioCard(modifier = modifier, onClick = onClick) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(color.copy(alpha = .13f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, color = color, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(12.dp))
        Text(title, color = AppText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = AppTextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun HeaderStatus(text: String, color: Color = AppSuccess) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SegmentRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { item ->
            val active = item == selected
            Surface(
                onClick = { onSelect(item) },
                shape = RoundedCornerShape(50.dp),
                color = if (active) AppPrimary else StudioPanel,
                border = BorderStroke(1.dp, if (active) AppPrimary else AppBorder),
            ) {
                Text(
                    item,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    color = if (active) Color.White else AppTextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun SettingRow(
    icon: String,
    title: String,
    value: String,
    onClick: (() -> Unit)? = null,
    valueColor: Color = AppTextSecondary,
) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(AppPrimary.copy(alpha = .10f)),
            contentAlignment = Alignment.Center,
        ) { Text(icon, fontSize = 18.sp, color = AppPrimary) }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = AppText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            if (value.isNotBlank()) Text(value, color = valueColor, fontSize = 11.sp)
        }
        if (onClick != null) Text("›", color = AppTextMuted, fontSize = 24.sp)
    }
}

@Composable
private fun MetricTile(label: String, value: String, accent: Color = AppPrimary, modifier: Modifier = Modifier) {
    StudioCard(modifier = modifier) {
        Text(value, color = accent, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(3.dp))
        Text(label, color = AppTextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun PreviewCanvas(title: String, subtitle: String, badge: String? = null) {
    Box(
        Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(17.dp))
            .background(StudioPanelRaised)
            .border(1.dp, AppPrimary.copy(alpha = .45f), RoundedCornerShape(17.dp)),
    ) {
        Box(
            Modifier.fillMaxSize().background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(Color(0xFF07141D), Color(0xFF0A2B3A), Color(0xFF071116))
                )
            )
        )
        Column(Modifier.align(Alignment.BottomStart).padding(15.dp)) {
            if (badge != null) {
                Surface(color = AppLive, shape = RoundedCornerShape(6.dp)) {
                    Text(badge, color = Color.White, fontWeight = FontWeight.Black, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
                Spacer(Modifier.height(7.dp))
            }
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(subtitle, color = AppTextSecondary, fontSize = 11.sp)
        }
        Text("UL", color = AppPrimary.copy(alpha = .35f), fontSize = 54.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun Phase17StudioScreen(
    sceneState: SceneState,
    overlayState: OverlayState,
    facecamState: FacecamState,
    onRoute: (AppRoute) -> Unit,
    onDestination: (AppDestination) -> Unit,
) {
    LockedStudioPage(
        phase = "CREATOR STUDIO",
        title = "Your creator workspace",
        subtitle = "Prepare scenes, overlays, sound, camera, and stream quality from one professional control center.",
    ) {
        PreviewCanvas(
            title = "${sceneState.activeScene.name} workspace",
            subtitle = "${sceneState.scenes.size} scenes · ${overlayState.layers.size} overlays · Facecam ${if (facecamState.config.enabled) "on" else "off"}",
        )
        Spacer(Modifier.height(14.dp))
        UlPrimaryButton("Create New Setup", onClick = { onRoute(AppRoute.SceneLibrary) })
        Spacer(Modifier.height(20.dp))

        Text("Workspace", color = AppText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolCard("▣", "Scenes", "Build and switch layouts", onClick = { onRoute(AppRoute.SceneLibrary) }, modifier = Modifier.weight(1f))
            ToolCard("✦", "Overlays", "Brand your stream", Purple, { onDestination(AppDestination.Overlays) }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolCard("≋", "Audio Mixer", "Balance every source", Blue, { onRoute(AppRoute.AudioMixer) }, Modifier.weight(1f))
            ToolCard("◉", "Facecam", "Frame your presence", Pink, { onRoute(AppRoute.FacecamEditor) }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolCard("HD", "Stream Quality", "Optimize your broadcast", Orange, { onRoute(AppRoute.QualityCenter) }, Modifier.weight(1f))
            ToolCard("↗", "Stream History", "Review past sessions", AppSuccess, { onDestination(AppDestination.Activity) }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))

        Text("Creator shortcuts", color = AppText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        StudioCard {
            SettingRow("+", "Create scene", "Start from a clean 16:9 canvas", onClick = { onRoute(AppRoute.SceneLibrary) })
            HorizontalDivider(color = AppBorder)
            SettingRow("□", "Edit current scene", sceneState.activeScene.name, onClick = { onRoute(AppRoute.SceneEditor) })
            HorizontalDivider(color = AppBorder)
            SettingRow("◎", "Test stream", "Run setup and preflight", onClick = { onRoute(AppRoute.StreamDetails) })
            HorizontalDivider(color = AppBorder)
            SettingRow("▶", "Go Live", "Use your saved destinations", onClick = { onDestination(AppDestination.GoLive) })
        }
    }
}

private enum class SceneView { LIBRARY, CATEGORIES, PREVIEW, ACTIONS, TEMPLATES, SWITCH }

@Composable
fun Phase18ScenesScreen(
    sceneState: SceneState,
    state: MobileIntegrationState? = null,
    onRoute: (AppRoute) -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        state?.refreshScenes()
        val cloud = state?.scenes.orEmpty()
        if (cloud.isNotEmpty()) {
            sceneState.replaceFromBackend(
                cloud.map { com.universallive.app.streaming.overlays.StreamScene(it.id, it.name, emptyList()) },
                state?.selectedScene?.id ?: cloud.firstOrNull { it.isDefault }?.id,
            )
        }
    }
    var view by remember { mutableStateOf(SceneView.LIBRARY) }
    var draftName by remember { mutableStateOf("") }
    val active = sceneState.activeScene

    LockedStudioPage(
        phase = "SCENES",
        title = when (view) {
            SceneView.LIBRARY -> "Scenes"
            SceneView.CATEGORIES -> "Scene categories"
            SceneView.PREVIEW -> "Scene preview"
            SceneView.ACTIONS -> "Scene actions"
            SceneView.TEMPLATES -> "Scene templates"
            SceneView.SWITCH -> "Quick switch"
        },
        subtitle = "Build your live look, organize layouts, and switch scenes without leaving your creator workspace.",
        onBack = { if (view == SceneView.LIBRARY) onBack() else view = SceneView.LIBRARY },
    ) {
        SegmentRow(listOf("Library", "Categories", "Preview", "Actions", "Templates", "Quick Switch"), when (view) {
            SceneView.LIBRARY -> "Library"; SceneView.CATEGORIES -> "Categories"; SceneView.PREVIEW -> "Preview"; SceneView.ACTIONS -> "Actions"; SceneView.TEMPLATES -> "Templates"; SceneView.SWITCH -> "Quick Switch"
        }) {
            view = when (it) {
                "Categories" -> SceneView.CATEGORIES; "Preview" -> SceneView.PREVIEW; "Actions" -> SceneView.ACTIONS; "Templates" -> SceneView.TEMPLATES; "Quick Switch" -> SceneView.SWITCH; else -> SceneView.LIBRARY
            }
        }
        Spacer(Modifier.height(18.dp))

        when (view) {
            SceneView.LIBRARY -> {
                UlTextField(draftName, { draftName = it }, "New scene name", placeholder = "Gaming Scene")
                Spacer(Modifier.height(10.dp))
                UlPrimaryButton("+ Create New Scene", onClick = {
                    val nextName = draftName.ifBlank { "Scene ${sceneState.scenes.size + 1}" }
                    if (state == null) {
                        sceneState.add(nextName, emptyList())
                    } else {
                        scope.launch {
                            state.addCloudScene(nextName)
                            val cloud = state.scenes
                            if (cloud.isNotEmpty()) sceneState.replaceFromBackend(
                                cloud.map { com.universallive.app.streaming.overlays.StreamScene(it.id, it.name, emptyList()) },
                                state.selectedScene?.id,
                            )
                        }
                    }
                    draftName = ""
                })
                Spacer(Modifier.height(18.dp))
                sceneState.scenes.forEach { scene ->
                    StudioCard(selected = scene.id == sceneState.activeSceneId, onClick = { sceneState.activate(scene.id); scope.launch { state?.activateStudioScene(scene.id, sceneState) }; view = SceneView.PREVIEW }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(AppPrimary.copy(alpha = .10f)), contentAlignment = Alignment.Center) { Text("▣", color = AppPrimary, fontSize = 22.sp) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(scene.name, color = AppText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text("1920×1080 · ${if (scene.id == sceneState.activeSceneId) "Live ready" else "Saved"}", color = if (scene.id == sceneState.activeSceneId) AppSuccess else AppTextSecondary, fontSize = 11.sp)
                            }
                            Text("›", color = AppTextMuted, fontSize = 24.sp)
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                }
            }
            SceneView.CATEGORIES -> {
                listOf("Starting & Ending" to "Intros, BRB, outros", "Gaming" to "Gameplay, facecam, overlays", "Just Chatting" to "Camera, mic, socials", "Podcasts" to "Guests and clean framing", "Product Demos" to "Screen, camera, callouts", "Custom" to "Build your own").forEach { (name, desc) ->
                    StudioCard(onClick = { draftName = name; view = SceneView.TEMPLATES }) {
                        SettingRow("▦", name, desc)
                    }
                    Spacer(Modifier.height(9.dp))
                }
            }
            SceneView.PREVIEW -> {
                PreviewCanvas(active.name, "1920×1080 · ${active.layerIds.size} linked layers", "LIVE READY")
                Spacer(Modifier.height(14.dp))
                StudioCard {
                    SettingRow("◉", "Camera", "Optional facecam")
                    SettingRow("▣", "Screen capture", "Gameplay or app")
                    SettingRow("✦", "Overlays", "Branding and labels")
                    SettingRow("≋", "Audio", "Mic + game audio")
                }
                Spacer(Modifier.height(12.dp))
                UlPrimaryButton("Edit Scene", onClick = { onRoute(AppRoute.SceneEditor) })
            }
            SceneView.ACTIONS -> {
                StudioCard {
                    SettingRow("✎", "Edit scene", "Open the visual editor", onClick = { onRoute(AppRoute.SceneEditor) })
                    HorizontalDivider(color = AppBorder)
                    SettingRow("▣", "Duplicate scene", "Create a working copy", onClick = {
                        if (state == null) sceneState.add("${active.name} Copy", active.layerIds) else scope.launch {
                            state.selectedScene = state.scenes.firstOrNull { it.id == active.id }
                            state.duplicateSelectedScene()
                            val cloud = state.scenes
                            if (cloud.isNotEmpty()) sceneState.replaceFromBackend(cloud.map { com.universallive.app.streaming.overlays.StreamScene(it.id, it.name, emptyList()) }, state.selectedScene?.id)
                        }
                    })
                    HorizontalDivider(color = AppBorder)
                    SettingRow("↕", "Set active", "Use this as your current layout", onClick = { sceneState.activate(active.id); scope.launch { state?.activateStudioScene(active.id, sceneState) } })
                    HorizontalDivider(color = AppBorder)
                    SettingRow("⌫", "Delete scene", if (sceneState.scenes.size > 1) "Remove this scene" else "Keep at least one scene", onClick = {
                        if (state == null) sceneState.delete(active.id) else scope.launch {
                            state.selectedScene = state.scenes.firstOrNull { it.id == active.id }
                            state.removeSelectedScene()
                            val cloud = state.scenes
                            if (cloud.isNotEmpty()) sceneState.replaceFromBackend(cloud.map { com.universallive.app.streaming.overlays.StreamScene(it.id, it.name, emptyList()) }, state.selectedScene?.id)
                        }
                    })
                }
            }
            SceneView.TEMPLATES -> {
                listOf("Streaming Essentials" to "Screen + camera + labels", "Pro Gaming" to "Gameplay + facecam + overlay", "Just Chatting" to "Clean camera layout", "Podcast Studio" to "Voice-first layout", "Product Showcase" to "Screen + camera + callout", "Event Stage" to "Branded live event").forEach { (name, desc) ->
                    StudioCard {
                        Text(name, color = AppText, fontWeight = FontWeight.Bold)
                        Text(desc, color = AppTextSecondary, fontSize = 12.sp)
                        Spacer(Modifier.height(10.dp))
                        UlSecondaryButton("Use Template", onClick = {
                            if (state == null) sceneState.add(name, emptyList()) else scope.launch { state.addCloudScene(name) }
                            view = SceneView.PREVIEW
                        })
                    }
                    Spacer(Modifier.height(9.dp))
                }
            }
            SceneView.SWITCH -> {
                sceneState.scenes.forEachIndexed { index, scene ->
                    StudioCard(selected = scene.id == sceneState.activeSceneId, onClick = { sceneState.activate(scene.id); scope.launch { state?.activateStudioScene(scene.id, sceneState) } }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}", color = AppPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(scene.name, color = AppText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            if (scene.id == sceneState.activeSceneId) HeaderStatus("ACTIVE")
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                }
                UlPrimaryButton("Switch to ${active.name}", onClick = { sceneState.activate(active.id); scope.launch { state?.activateStudioScene(active.id, sceneState) } })
            }
        }
    }
}

private enum class EditorView { CANVAS, SOURCES, ADD, CONTROLS, LAYERS, SAVE }

@Composable
fun Phase19SceneEditorScreen(
    sceneState: SceneState,
    overlayState: OverlayState,
    state: MobileIntegrationState? = null,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var view by remember { mutableStateOf(EditorView.CANVAS) }
    var selectedId by remember { mutableStateOf<String?>(overlayState.layers.lastOrNull()?.id) }
    var textDraft by remember { mutableStateOf("GOOD VIBES\nBIGGER STREAMS") }
    var layoutName by remember { mutableStateOf(sceneState.activeScene.name) }
    val selected = overlayState.layers.firstOrNull { it.id == selectedId }

    fun addLayer(kind: OverlayKind, label: String, text: String = "") {
        val id = "layer-${overlayState.layers.size + 1}"
        overlayState.upsert(OverlayLayer(id = id, kind = kind, label = label, text = text))
        selectedId = id
        view = EditorView.SOURCES
    }

    LockedStudioPage(
        phase = "SCENE EDITOR",
        title = when (view) {
            EditorView.CANVAS -> "Scene Editor"; EditorView.SOURCES -> "Sources"; EditorView.ADD -> "Add Source"; EditorView.CONTROLS -> "Element Controls"; EditorView.LAYERS -> "Layer Order"; EditorView.SAVE -> "Save Layout"
        },
        subtitle = "Arrange sources, customize elements, and build a polished live composition.",
        onBack = { if (view == EditorView.CANVAS) onBack() else view = EditorView.CANVAS },
    ) {
        SegmentRow(listOf("Canvas", "Sources", "Add", "Controls", "Layers", "Save"), when (view) {
            EditorView.CANVAS -> "Canvas"; EditorView.SOURCES -> "Sources"; EditorView.ADD -> "Add"; EditorView.CONTROLS -> "Controls"; EditorView.LAYERS -> "Layers"; EditorView.SAVE -> "Save"
        }) { item ->
            view = when (item) { "Sources" -> EditorView.SOURCES; "Add" -> EditorView.ADD; "Controls" -> EditorView.CONTROLS; "Layers" -> EditorView.LAYERS; "Save" -> EditorView.SAVE; else -> EditorView.CANVAS }
        }
        Spacer(Modifier.height(18.dp))

        when (view) {
            EditorView.CANVAS -> {
                PreviewCanvas(sceneState.activeScene.name, "${overlayState.layers.size} overlay layers", "PREVIEW")
                Spacer(Modifier.height(12.dp))
                overlayState.layers.take(4).forEach { layer ->
                    StudioCard(selected = layer.id == selectedId, onClick = { selectedId = layer.id; view = EditorView.CONTROLS }) {
                        Text(layer.label, color = AppText, fontWeight = FontWeight.Bold)
                        Text("${layer.kind.name.lowercase()} · ${if (layer.enabled) "visible" else "hidden"}", color = AppTextSecondary, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UlSecondaryButton("Preview", {}, Modifier.weight(1f))
                    UlPrimaryButton("Go Live Scene", { view = EditorView.SAVE }, Modifier.weight(1f))
                }
            }
            EditorView.SOURCES -> {
                UlPrimaryButton("+ Add Source", onClick = { view = EditorView.ADD })
                Spacer(Modifier.height(14.dp))
                if (overlayState.layers.isEmpty()) {
                    StudioCard { Text("No custom sources yet. Add text, image, logo, or alert layers.", color = AppTextSecondary) }
                }
                overlayState.layers.forEach { layer ->
                    StudioCard(selected = layer.id == selectedId, onClick = { selectedId = layer.id; view = EditorView.CONTROLS }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (layer.kind == OverlayKind.TEXT) "T" else "▧", color = AppPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(layer.label, color = AppText, fontWeight = FontWeight.Bold)
                                Text(layer.kind.name.lowercase(), color = AppTextSecondary, fontSize = 11.sp)
                            }
                            Switch(checked = layer.enabled, onCheckedChange = { overlayState.upsert(layer.copy(enabled = it)) })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            EditorView.ADD -> {
                listOf(
                    Triple("◉", "Camera / Facecam", OverlayKind.IMAGE), Triple("▣", "Screen Capture", OverlayKind.IMAGE), Triple("T", "Text", OverlayKind.TEXT), Triple("▧", "Image / Logo", OverlayKind.LOGO), Triple("✦", "Overlay Widget", OverlayKind.IMAGE), Triple("◌", "Background", OverlayKind.IMAGE)
                ).forEach { (icon, label, kind) ->
                    StudioCard(onClick = { addLayer(kind, label, if (kind == OverlayKind.TEXT) textDraft else "") }) { SettingRow(icon, label, "Add to ${sceneState.activeScene.name}") }
                    Spacer(Modifier.height(8.dp))
                }
            }
            EditorView.CONTROLS -> {
                if (selected == null) {
                    StudioCard { Text("Select a source first.", color = AppTextSecondary) }
                } else {
                    StudioCard {
                        Text(selected.label, color = AppText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Opacity ${(selected.opacity * 100).toInt()}%", color = AppTextSecondary)
                        Slider(value = selected.opacity, onValueChange = { overlayState.upsert(selected.copy(opacity = it)) }, valueRange = 0.2f..1f)
                        Text("Width ${(selected.width * 100).toInt()}%", color = AppTextSecondary)
                        Slider(value = selected.width, onValueChange = { overlayState.upsert(selected.copy(width = it)) }, valueRange = 0.1f..0.8f)
                        if (selected.kind == OverlayKind.TEXT) {
                            Spacer(Modifier.height(8.dp))
                            UlTextField(selected.text, { overlayState.upsert(selected.copy(text = it)) }, "Text")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    UlPrimaryButton("Apply Changes", onClick = { view = EditorView.CANVAS })
                }
            }
            EditorView.LAYERS -> {
                overlayState.layers.reversed().forEachIndexed { index, layer ->
                    StudioCard(selected = layer.id == selectedId, onClick = { selectedId = layer.id }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}", color = AppPrimary, fontWeight = FontWeight.Black)
                            Spacer(Modifier.width(10.dp))
                            Text(layer.label, color = AppText, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            TextButton(onClick = { overlayState.move(layer.id, 1) }) { Text("Up") }
                            TextButton(onClick = { overlayState.move(layer.id, -1) }) { Text("Down") }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            EditorView.SAVE -> {
                PreviewCanvas(sceneState.activeScene.name, "Save and apply your layout", "READY")
                Spacer(Modifier.height(14.dp))
                UlTextField(layoutName, { layoutName = it }, "Layout Name")
                Spacer(Modifier.height(12.dp))
                StudioCard {
                    SettingRow("☑", "Save to My Layouts", "Keeps this scene locally")
                    SettingRow("★", "Set as active scene", sceneState.activeScene.name)
                    SettingRow("◉", "Apply to live stream", "Available during an active session")
                }
                Spacer(Modifier.height(12.dp))
                UlPrimaryButton("Save & Apply", onClick = {
                    if (layoutName.isNotBlank() && layoutName != sceneState.activeScene.name && state == null) sceneState.add(layoutName, overlayState.layers.map { it.id })
                    scope.launch { state?.persistOverlayState(sceneState.activeScene.id, overlayState) }
                    view = EditorView.CANVAS
                })
            }
        }
    }
}

private enum class OverlayView { LIBRARY, ALERTS, LOWER, CHAT, BRANDING, APPLY }

@Composable
fun Phase20OverlaysScreen(
    overlayState: OverlayState,
    sceneState: SceneState,
    state: MobileIntegrationState? = null,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var view by remember { mutableStateOf(OverlayView.LIBRARY) }
    var lowerName by remember { mutableStateOf("Alex Rivers") }
    var lowerSub by remember { mutableStateOf("Content Creator") }
    var chatEnabled by remember { mutableStateOf(true) }
    var brandAccent by remember { mutableStateOf("Cyan Glow") }

    fun ensureLayer(id: String, kind: OverlayKind, label: String, text: String) {
        val current = overlayState.layers.firstOrNull { it.id == id }
        overlayState.upsert(current?.copy(enabled = true, text = text) ?: OverlayLayer(id, kind, label, text = text))
    }

    LockedStudioPage(
        phase = "OVERLAYS",
        title = when (view) { OverlayView.LIBRARY -> "Overlays"; OverlayView.ALERTS -> "Alert Packages"; OverlayView.LOWER -> "Lower Third Editor"; OverlayView.CHAT -> "Chat Overlay"; OverlayView.BRANDING -> "Branding & Style"; OverlayView.APPLY -> "Apply to Scene" },
        subtitle = "Alerts, labels, widgets, and visual identity that make your stream unmistakably yours.",
        onBack = { if (view == OverlayView.LIBRARY) onBack() else view = OverlayView.LIBRARY },
    ) {
        SegmentRow(listOf("Library", "Alerts", "Lower Third", "Chat", "Branding", "Apply"), when (view) { OverlayView.LIBRARY -> "Library"; OverlayView.ALERTS -> "Alerts"; OverlayView.LOWER -> "Lower Third"; OverlayView.CHAT -> "Chat"; OverlayView.BRANDING -> "Branding"; OverlayView.APPLY -> "Apply" }) {
            view = when (it) { "Alerts" -> OverlayView.ALERTS; "Lower Third" -> OverlayView.LOWER; "Chat" -> OverlayView.CHAT; "Branding" -> OverlayView.BRANDING; "Apply" -> OverlayView.APPLY; else -> OverlayView.LIBRARY }
        }
        Spacer(Modifier.height(18.dp))
        when (view) {
            OverlayView.LIBRARY -> {
                listOf("Neon Core" to "Animated cyan frame", "Minimal" to "Clean lower-third system", "Cyber" to "High-energy accent pack", "Retro" to "Pixel inspired", "Streamer" to "Creator essentials", "Pro Pack" to "Premium broadcast look").forEachIndexed { index, pair ->
                    StudioCard(onClick = { brandAccent = pair.first; view = OverlayView.APPLY }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(listOf(AppPrimary, Purple, Pink, AppSuccess)[index % 4].copy(alpha=.15f)), contentAlignment=Alignment.Center) { Text("✦", color=listOf(AppPrimary, Purple, Pink, AppSuccess)[index % 4], fontSize=23.sp) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) { Text(pair.first, color=AppText, fontWeight=FontWeight.Bold); Text(pair.second, color=AppTextSecondary, fontSize=11.sp) }
                            Text("›", color=AppTextMuted, fontSize=24.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            OverlayView.ALERTS -> {
                listOf("Neon Pulse", "Clean Essentials", "Cyber FX", "Pixel Vibes", "Creator Pro").forEach { name ->
                    StudioCard {
                        Text(name, color = AppText, fontWeight = FontWeight.Bold)
                        Text("Subscriber · Follow · Donation alerts", color = AppTextSecondary, fontSize = 11.sp)
                        Spacer(Modifier.height(8.dp))
                        UlSecondaryButton("Enable Package", onClick = { ensureLayer("alerts", OverlayKind.IMAGE, "Alert Widget", name) })
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            OverlayView.LOWER -> {
                PreviewCanvas(lowerName, lowerSub, "LOWER THIRD")
                Spacer(Modifier.height(12.dp))
                UlTextField(lowerName, { lowerName = it }, "Display Name")
                Spacer(Modifier.height(8.dp))
                UlTextField(lowerSub, { lowerSub = it }, "Subtitle")
                Spacer(Modifier.height(12.dp))
                UlPrimaryButton("Save Lower Third", onClick = { ensureLayer("lower-third", OverlayKind.TEXT, "Lower Third", "$lowerName — $lowerSub") })
            }
            OverlayView.CHAT -> {
                PreviewCanvas("Live Chat", "Viewer messages appear here", "CHAT")
                Spacer(Modifier.height(12.dp))
                StudioCard {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("Show chat background", color = AppText, modifier = Modifier.weight(1f)); Switch(chatEnabled, { chatEnabled = it }) }
                    Text("Message opacity", color = AppTextSecondary)
                    Slider(.80f, onValueChange = {})
                }
                Spacer(Modifier.height(12.dp))
                UlPrimaryButton(if (chatEnabled) "Apply Chat Overlay" else "Keep Chat Hidden", onClick = { if (chatEnabled) ensureLayer("chat", OverlayKind.TEXT, "Chat Overlay", "Live Chat") else overlayState.remove("chat") })
            }
            OverlayView.BRANDING -> {
                Text("Brand Colors", color=AppText, fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(AppPrimary, Purple, Pink, Orange, AppSuccess).forEach { color -> Box(Modifier.size(42.dp).clip(CircleShape).background(color).border(2.dp, Color.White.copy(alpha=.15f), CircleShape)) }
                }
                Spacer(Modifier.height(16.dp))
                Text("Overlay Presets", color=AppText, fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                SegmentRow(listOf("Cyan Glow", "Purple Haze", "Minimal", "Bold"), brandAccent) { brandAccent = it }
                Spacer(Modifier.height(14.dp))
                StudioCard { Text("Selected: $brandAccent", color=AppPrimary, fontWeight=FontWeight.Bold); Text("This visual choice is stored in the current UI state until backend persistence is added.", color=AppTextSecondary, fontSize=11.sp) }
            }
            OverlayView.APPLY -> {
                PreviewCanvas(sceneState.activeScene.name, "${overlayState.layers.size} active overlays · $brandAccent", "SCENE")
                Spacer(Modifier.height(12.dp))
                StudioCard {
                    listOf("Alerts" to "alerts", "Lower Third" to "lower-third", "Chat Overlay" to "chat").forEach { (label,id) ->
                        val layer=overlayState.layers.firstOrNull{it.id==id}
                        Row(Modifier.fillMaxWidth().padding(vertical=7.dp), verticalAlignment=Alignment.CenterVertically) {
                            Text(label, color=AppText, modifier=Modifier.weight(1f)); Switch(layer?.enabled==true, onCheckedChange={ enabled -> if(layer!=null) overlayState.upsert(layer.copy(enabled=enabled)) else if(enabled) ensureLayer(id, if(id=="lower-third") OverlayKind.TEXT else OverlayKind.IMAGE, label, label) })
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                UlPrimaryButton("Apply to ${sceneState.activeScene.name}", onClick = { scope.launch { state?.persistOverlayState(sceneState.activeScene.id, overlayState) }; view = OverlayView.LIBRARY })
            }
        }
    }
}

private enum class AudioView { MIXER, LEVELS, MIC, GAME, MONITORING, PRESETS }

@Composable
fun Phase21AudioMixerScreen(
    streamState: StreamConfigState,
    state: MobileIntegrationState? = null,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var view by remember { mutableStateOf(AudioView.MIXER) }
    var micLevel by remember { mutableStateOf(.70f) }
    var gameLevel by remember { mutableStateOf(.62f) }
    var musicLevel by remember { mutableStateOf(.40f) }
    var monitorLevel by remember { mutableStateOf(.70f) }
    var noiseSuppression by remember { mutableStateOf(true) }
    var compressor by remember { mutableStateOf(true) }
    var preset by remember { mutableStateOf("Streaming") }

    LockedStudioPage(
        phase = "AUDIO MIXER",
        title = when(view){AudioView.MIXER->"Audio Mixer";AudioView.LEVELS->"Live Audio Levels";AudioView.MIC->"Microphone Settings";AudioView.GAME->"Game Audio";AudioView.MONITORING->"Monitoring";AudioView.PRESETS->"Audio Presets"},
        subtitle = "Control microphone, game audio, monitoring, and your creator mix from one simple surface.",
        onBack = { if(view==AudioView.MIXER) onBack() else view=AudioView.MIXER },
    ) {
        SegmentRow(listOf("Mixer","Levels","Mic","Game","Monitoring","Presets"), when(view){AudioView.MIXER->"Mixer";AudioView.LEVELS->"Levels";AudioView.MIC->"Mic";AudioView.GAME->"Game";AudioView.MONITORING->"Monitoring";AudioView.PRESETS->"Presets"}) { view = when(it){"Levels"->AudioView.LEVELS;"Mic"->AudioView.MIC;"Game"->AudioView.GAME;"Monitoring"->AudioView.MONITORING;"Presets"->AudioView.PRESETS;else->AudioView.MIXER} }
        Spacer(Modifier.height(18.dp))
        when(view){
            AudioView.MIXER -> {
                listOf("Microphone" to micLevel, "Game Audio" to gameLevel, "Music" to musicLevel).forEach { (label, level) ->
                    StudioCard {
                        Row(verticalAlignment=Alignment.CenterVertically){Text(if(label=="Microphone") "♬" else "≋", color=AppPrimary, fontSize=20.sp);Spacer(Modifier.width(10.dp));Text(label,color=AppText,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));Text("${(level*100).toInt()}%",color=AppTextSecondary)}
                        Slider(level, onValueChange={ if(label=="Microphone") micLevel=it else if(label=="Game Audio") gameLevel=it else musicLevel=it })
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    ToolCard("♬","Microphone",if(streamState.config.microphoneEnabled)"Enabled" else "Disabled",AppPrimary,{view=AudioView.MIC},Modifier.weight(1f))
                    ToolCard("🎮","Game Audio",if(streamState.config.internalAudioEnabled)"Enabled" else "Disabled",Blue,{view=AudioView.GAME},Modifier.weight(1f))
                }
            }
            AudioView.LEVELS -> {
                StudioCard {
                    Text("Real-time meters",color=AppText,fontWeight=FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    listOf("Mic" to micLevel,"Game" to gameLevel,"Music" to musicLevel,"Master" to .78f).forEach { (label,level) -> Row(verticalAlignment=Alignment.CenterVertically){Text(label,color=AppTextSecondary,modifier=Modifier.width(64.dp));LinearProgressIndicator(progress={level},modifier=Modifier.weight(1f).height(8.dp).clip(CircleShape),color=if(level>.85f)AppWarning else AppSuccess,trackColor=AppSurfaceInteractive);Spacer(Modifier.width(8.dp));Text("${(level*100).toInt()}",color=AppTextMuted,fontSize=11.sp)};Spacer(Modifier.height(10.dp)) }
                }
            }
            AudioView.MIC -> {
                StudioCard {
                    Row(verticalAlignment=Alignment.CenterVertically){Text("Microphone",color=AppText,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));Switch(streamState.config.microphoneEnabled,streamState::setMicrophoneEnabled)}
                    Text("Input Gain",color=AppTextSecondary);Slider(micLevel,{micLevel=it})
                    Row(verticalAlignment=Alignment.CenterVertically){Text("Noise Suppression",color=AppText,modifier=Modifier.weight(1f));Switch(noiseSuppression,{noiseSuppression=it})}
                    Row(verticalAlignment=Alignment.CenterVertically){Text("Compressor",color=AppText,modifier=Modifier.weight(1f));Switch(compressor,{compressor=it})}
                }
            }
            AudioView.GAME -> {
                StudioCard {
                    Row(verticalAlignment=Alignment.CenterVertically){Text("Game / Device Audio",color=AppText,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));Switch(streamState.config.internalAudioEnabled,streamState::setInternalAudioEnabled)}
                    Text("Game Audio Level",color=AppTextSecondary);Slider(gameLevel,{gameLevel=it})
                    Text("Universal Live will use Android playback capture when supported by the selected app/game.",color=AppTextSecondary,fontSize=11.sp,lineHeight=16.sp)
                }
            }
            AudioView.MONITORING -> {
                StudioCard {
                    Text("Headphones",color=AppText,fontWeight=FontWeight.Bold)
                    Text("Monitor Volume ${(monitorLevel*100).toInt()}%",color=AppTextSecondary);Slider(monitorLevel,{monitorLevel=it})
                    SettingRow("♬","Mic Monitoring","Local preference")
                    SettingRow("🎮","Monitor Game Audio","Local preference")
                    SettingRow("♪","Monitor Music","Local preference")
                }
                Spacer(Modifier.height(12.dp));UlPrimaryButton("Save Audio Mix", onClick = { scope.launch { state?.persistAudio(streamState, preset) } })
            }
            AudioView.PRESETS -> {
                listOf("Streaming","Competitive Gaming","Podcast / Talk Show","Music & Chat","Just Chatting","Mobile Streaming").forEach { name ->
                    StudioCard(selected=name==preset,onClick={preset=name;scope.launch{state?.persistAudio(streamState,preset)}}) { Row(verticalAlignment=Alignment.CenterVertically){Text(if(name=="Streaming")"🎮" else "♬",fontSize=20.sp);Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(name,color=AppText,fontWeight=FontWeight.Bold);Text(if(name==preset)"Active mix" else "Tap to apply",color=if(name==preset)AppSuccess else AppTextSecondary,fontSize=11.sp)}} }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

private enum class FacecamView { SETUP, SOURCE, FRAME, BACKGROUND, STYLE, APPLY }

@Composable
fun Phase22FacecamScreen(
    facecamState: FacecamState,
    state: MobileIntegrationState? = null,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var view by remember { mutableStateOf(FacecamView.SETUP) }
    var background by remember { mutableStateOf("None") }
    var style by remember { mutableStateOf("Default") }

    LockedStudioPage(
        phase="CAMERA",
        title=when(view){FacecamView.SETUP->"Facecam";FacecamView.SOURCE->"Camera Source";FacecamView.FRAME->"Framing & Crop";FacecamView.BACKGROUND->"Background";FacecamView.STYLE->"Style Presets";FacecamView.APPLY->"Preview & Apply"},
        subtitle="Camera setup, framing, and presentation. Make your on-camera presence feel intentional.",
        onBack={if(view==FacecamView.SETUP)onBack()else view=FacecamView.SETUP},
    ) {
        SegmentRow(listOf("Setup","Source","Framing","Background","Style","Apply"),when(view){FacecamView.SETUP->"Setup";FacecamView.SOURCE->"Source";FacecamView.FRAME->"Framing";FacecamView.BACKGROUND->"Background";FacecamView.STYLE->"Style";FacecamView.APPLY->"Apply"}){view=when(it){"Source"->FacecamView.SOURCE;"Framing"->FacecamView.FRAME;"Background"->FacecamView.BACKGROUND;"Style"->FacecamView.STYLE;"Apply"->FacecamView.APPLY;else->FacecamView.SETUP}}
        Spacer(Modifier.height(18.dp))
        when(view){
            FacecamView.SETUP->{PreviewCanvas("Facecam Preview","${facecamState.config.lens.label} camera · ${facecamState.config.shape.label}",if(facecamState.config.enabled)"ENABLED" else "OFF");Spacer(Modifier.height(12.dp));StudioCard{Row(verticalAlignment=Alignment.CenterVertically){Text("Facecam Enabled",color=AppText,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));Switch(facecamState.config.enabled,facecamState::setEnabled)};SettingRow("✓","Camera source",facecamState.config.lens.label);SettingRow("✓","Shape",facecamState.config.shape.label);SettingRow("✓","Size",facecamState.config.sizeLabel)};Spacer(Modifier.height(12.dp));UlPrimaryButton("Open Camera Setup", onClick = {view=FacecamView.SOURCE})}
            FacecamView.SOURCE->{listOf(FacecamLens.FRONT,FacecamLens.BACK).forEach{lens->StudioCard(selected=facecamState.config.lens==lens,onClick={facecamState.setLens(lens)}){SettingRow("◉",if(lens==FacecamLens.FRONT)"Front Camera" else "Back Camera",if(lens==FacecamLens.FRONT)"Built-in front camera" else "Rear device camera")}};Spacer(Modifier.height(12.dp));UlPrimaryButton("Continue", onClick = {view=FacecamView.FRAME})}
            FacecamView.FRAME->{PreviewCanvas("Framing Preview","Position ${facecamState.config.positionLabel}");Spacer(Modifier.height(12.dp));StudioCard{Text("Size ${facecamState.config.sizeLabel}",color=AppTextSecondary);Slider(facecamState.config.size,onValueChange=facecamState::setSize,valueRange=.14f..0.42f);Text("Horizontal position",color=AppTextSecondary);Slider(facecamState.config.x,onValueChange={facecamState.setPosition(it,facecamState.config.y)},valueRange=0f..(1f-facecamState.config.size));Text("Vertical position",color=AppTextSecondary);Slider(facecamState.config.y,onValueChange={facecamState.setPosition(facecamState.config.x,it)},valueRange=0f..(1f-facecamState.config.size))};Spacer(Modifier.height(12.dp));UlPrimaryButton("Save Framing", onClick = {view=FacecamView.BACKGROUND})}
            FacecamView.BACKGROUND->{PreviewCanvas("Background: $background","Preview uses the selected camera style; unsupported effects remain disabled during broadcast.");Spacer(Modifier.height(12.dp));SegmentRow(listOf("None","Blur","Chroma","Remove","Studio","Gaming"),background){background=it};Spacer(Modifier.height(12.dp));UlPrimaryButton("Continue", onClick = {view=FacecamView.STYLE})}
            FacecamView.STYLE->{listOf(FacecamShape.ROUNDED,FacecamShape.CIRCLE,FacecamShape.SQUARE).forEach{shape->StudioCard(selected=facecamState.config.shape==shape,onClick={facecamState.setShape(shape);style=shape.label}){Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(44.dp).clip(if(shape==FacecamShape.CIRCLE)CircleShape else RoundedCornerShape(if(shape==FacecamShape.ROUNDED)14.dp else 3.dp)).background(AppPrimary.copy(alpha=.18f)));Spacer(Modifier.width(12.dp));Text(shape.label,color=AppText,fontWeight=FontWeight.Bold)}};Spacer(Modifier.height(8.dp))};Spacer(Modifier.height(12.dp));UlPrimaryButton("Preview & Apply", onClick = {view=FacecamView.APPLY})}
            FacecamView.APPLY->{PreviewCanvas("Facecam $style","$background background · ${facecamState.config.sizeLabel}","SCENE PREVIEW");Spacer(Modifier.height(12.dp));SegmentRow(listOf("Top Left","Top Right","Bottom Left","Bottom Right"),"Top Right"){pos->when(pos){"Top Left"->facecamState.setPosition(.04f,.05f);"Bottom Left"->facecamState.setPosition(.04f,.70f);"Bottom Right"->facecamState.setPosition(.72f,.70f);else->facecamState.setPosition(.72f,.05f)}};Spacer(Modifier.height(12.dp));UlPrimaryButton("Apply to Scene", onClick = {facecamState.setEnabled(true);scope.launch{state?.persistFacecam(facecamState,background)};view=FacecamView.SETUP})}
        }
    }
}

private enum class QualityView { OVERVIEW, PRESETS, MANUAL, PERFORMANCE, NETWORK, SUMMARY }

@Composable
fun Phase23StreamQualityScreen(
    streamState: StreamConfigState,
    captureController: CaptureController,
    state: MobileIntegrationState? = null,
    onGoLive: () -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var view by remember { mutableStateOf(QualityView.OVERVIEW) }
    var adaptive by remember { mutableStateOf(true) }
    val config=streamState.config
    val snap=captureController.snapshot

    LockedStudioPage(
        phase="STREAM QUALITY",
        title=when(view){QualityView.OVERVIEW->"Stream Quality";QualityView.PRESETS->"Recommended Presets";QualityView.MANUAL->"Manual Settings";QualityView.PERFORMANCE->"Device Performance";QualityView.NETWORK->"Network Recommendations";QualityView.SUMMARY->"Go Live Ready"},
        subtitle="Balance resolution, frame rate, bitrate, and device/network performance before you broadcast.",
        onBack={if(view==QualityView.OVERVIEW)onBack()else view=QualityView.OVERVIEW},
    ) {
        SegmentRow(listOf("Overview","Presets","Manual","Device","Network","Summary"),when(view){QualityView.OVERVIEW->"Overview";QualityView.PRESETS->"Presets";QualityView.MANUAL->"Manual";QualityView.PERFORMANCE->"Device";QualityView.NETWORK->"Network";QualityView.SUMMARY->"Summary"}){view=when(it){"Presets"->QualityView.PRESETS;"Manual"->QualityView.MANUAL;"Device"->QualityView.PERFORMANCE;"Network"->QualityView.NETWORK;"Summary"->QualityView.SUMMARY;else->QualityView.OVERVIEW}}
        Spacer(Modifier.height(18.dp))
        when(view){
            QualityView.OVERVIEW->{PreviewCanvas("${config.resolution.label} · ${config.fps.value} FPS","${config.bitrateKbps} Kbps · ${config.orientation.label}","${if(snap.isPublishing)"LIVE" else "READY"}");Spacer(Modifier.height(12.dp));StudioCard{SettingRow("⚙","Optimize stream quality","Recommended presets", onClick = {view=QualityView.PRESETS});SettingRow("▣","Check device & network","Performance and stability", onClick = {view=QualityView.PERFORMANCE});SettingRow("≡","Fine-tune manually","Control every setting", onClick = {view=QualityView.MANUAL})};Spacer(Modifier.height(12.dp));UlPrimaryButton("Run Quality Check", onClick = {view=QualityView.SUMMARY})}
            QualityView.PRESETS->{listOf("Auto (Recommended)" to Triple(StreamResolution.P1080,StreamFps.FPS30,6800),"1080p 60 FPS" to Triple(StreamResolution.P1080,StreamFps.FPS60,8000),"720p 60 FPS" to Triple(StreamResolution.P720,StreamFps.FPS60,4500),"1080p 30 FPS" to Triple(StreamResolution.P1080,StreamFps.FPS30,6000),"480p 30 FPS" to Triple(StreamResolution.P480,StreamFps.FPS30,2000)).forEach{(name,p)->val selected=config.resolution==p.first&&config.fps==p.second&&config.bitrateKbps==p.third;StudioCard(selected=selected,onClick={streamState.setResolution(p.first);streamState.setFps(p.second);streamState.setBitrateKbps(p.third)}){Text(name,color=AppText,fontWeight=FontWeight.Bold);Text("${p.first.label} · ${p.second.value} FPS · ${p.third} Kbps",color=if(selected)AppSuccess else AppTextSecondary,fontSize=11.sp)};Spacer(Modifier.height(8.dp))};UlPrimaryButton("Apply Preset", onClick = {scope.launch{state?.persistQuality(streamState,adaptive)};view=QualityView.SUMMARY})}
            QualityView.MANUAL->{StudioCard{Text("Resolution",color=AppTextSecondary);SegmentRow(StreamResolution.entries.map{it.label},config.resolution.label){label->StreamResolution.entries.firstOrNull{it.label==label}?.let(streamState::setResolution)};Spacer(Modifier.height(12.dp));Text("Frame Rate",color=AppTextSecondary);SegmentRow(StreamFps.entries.map{"${it.value} FPS"},"${config.fps.value} FPS"){label->streamState.setFps(if(label.startsWith("60"))StreamFps.FPS60 else StreamFps.FPS30)};Spacer(Modifier.height(12.dp));Text("Bitrate ${config.bitrateKbps} Kbps",color=AppTextSecondary);Slider(config.bitrateKbps.toFloat(),onValueChange={streamState.setBitrateKbps((it/100).toInt()*100)},valueRange=1500f..12000f);Row(verticalAlignment=Alignment.CenterVertically){Text("Adaptive Bitrate",color=AppText,modifier=Modifier.weight(1f));Switch(adaptive,{adaptive=it;captureController.setAdaptiveBitrateEnabled(it)})};Text("Orientation",color=AppTextSecondary);SegmentRow(StreamOrientation.entries.map{it.label},config.orientation.label){label->StreamOrientation.entries.firstOrNull{it.label==label}?.let(streamState::setOrientation)}};Spacer(Modifier.height(12.dp));UlPrimaryButton("Save Settings", onClick = {scope.launch{state?.persistQuality(streamState,adaptive)};view=QualityView.SUMMARY})}
            QualityView.PERFORMANCE->{StudioCard{HeaderStatus("Ready to Stream");Spacer(Modifier.height(12.dp));listOf("Encoder" to (snap.encoderName.ifBlank{"H.264"}),"Video pipeline" to if(snap.isActive)"Active" else "Ready","Audio pipeline" to if(config.microphoneEnabled||config.internalAudioEnabled)"Configured" else "Disabled","Frame target" to "${config.fps.value} FPS").forEach{(a,b)->SettingRow("✓",a,b)}};Spacer(Modifier.height(12.dp));UlPrimaryButton("Run Performance Test", onClick = {view=QualityView.NETWORK})}
            QualityView.NETWORK->{StudioCard{Text("Network readiness",color=AppText,fontWeight=FontWeight.Bold);Spacer(Modifier.height(10.dp));val kbps=(snap.networkBitrateBps/1000).coerceAtLeast(0);MetricTile("Current outgoing",if(kbps>0)"$kbps Kbps" else "Not live");Spacer(Modifier.height(8.dp));HeaderStatus(if(kbps==0L||kbps>=config.bitrateKbps*.75)"Connection looks suitable" else "Below target bitrate",if(kbps==0L||kbps>=config.bitrateKbps*.75)AppSuccess else AppWarning);Spacer(Modifier.height(8.dp));Text("A wired or strong Wi-Fi connection is recommended for sustained 1080p streaming.",color=AppTextSecondary,fontSize=11.sp,lineHeight=16.sp)};Spacer(Modifier.height(12.dp));UlPrimaryButton("Continue", onClick = {view=QualityView.SUMMARY})}
            QualityView.SUMMARY->{PreviewCanvas("Stream Quality: Excellent","${config.resolution.label} · ${config.fps.value} FPS · ${config.bitrateKbps} Kbps","READY");Spacer(Modifier.height(12.dp));StudioCard{SettingRow("▣","Resolution",config.resolution.label);SettingRow("◌","Frame Rate","${config.fps.value} FPS");SettingRow("↗","Video Bitrate","${config.bitrateKbps} Kbps");SettingRow("↔","Orientation",config.orientation.label);SettingRow("⚙","Encoder",snap.encoderName.ifBlank{"H.264"})};Spacer(Modifier.height(12.dp));UlPrimaryButton("Proceed to Go Live",onClick={scope.launch{state?.persistQuality(streamState,adaptive)};onGoLive()})}
        }
    }
}

private enum class HistoryView { OVERVIEW, FILTERS, SESSIONS, SEARCH, HIGHLIGHTS }

@Composable
fun Phase24StreamHistoryScreen(
    state: MobileIntegrationState,
    onOpenDetails: (String) -> Unit,
    onDestination: (AppDestination) -> Unit,
) {
    var view by remember { mutableStateOf(HistoryView.OVERVIEW) }
    var filter by remember { mutableStateOf("All") }
    var query by remember { mutableStateOf("") }
    val scope=rememberCoroutineScope()
    LaunchedEffect(Unit){state.refreshHistory()}
    val filtered=state.streamHistory.filter{item->(filter=="All"||item.status.equals(filter,true))&&(query.isBlank()||item.title.orEmpty().contains(query,true))}

    LockedStudioPage(
        phase="STREAM HISTORY",
        title=when(view){HistoryView.OVERVIEW->"Stream History";HistoryView.FILTERS->"Filters";HistoryView.SESSIONS->"Stream Sessions";HistoryView.SEARCH->"Search History";HistoryView.HIGHLIGHTS->"Recent Highlights"},
        subtitle="Review previous broadcasts, performance, and outcomes. Learn, improve, and go further.",
    ) {
        SegmentRow(listOf("Overview","Filters","Sessions","Search","Highlights"),when(view){HistoryView.OVERVIEW->"Overview";HistoryView.FILTERS->"Filters";HistoryView.SESSIONS->"Sessions";HistoryView.SEARCH->"Search";HistoryView.HIGHLIGHTS->"Highlights"}){view=when(it){"Filters"->HistoryView.FILTERS;"Sessions"->HistoryView.SESSIONS;"Search"->HistoryView.SEARCH;"Highlights"->HistoryView.HIGHLIGHTS;else->HistoryView.OVERVIEW}}
        Spacer(Modifier.height(18.dp))
        when(view){
            HistoryView.OVERVIEW->{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){MetricTile("Total",state.streamHistory.size.toString(),modifier=Modifier.weight(1f));MetricTile("Completed",state.streamHistory.count{it.status.equals("ended",true)||it.status.equals("completed",true)}.toString(),AppSuccess,Modifier.weight(1f));MetricTile("Failed",state.streamHistory.count{it.status.contains("fail",true)||it.status.contains("error",true)}.toString(),AppLive,Modifier.weight(1f))};Spacer(Modifier.height(14.dp));HistoryList(state.streamHistory.take(6),onOpenDetails);if(state.streamHistory.isEmpty()){StudioCard{Text("No stream history yet. Your completed sessions will appear here after backend session sync.",color=AppTextSecondary)}};Spacer(Modifier.height(12.dp));UlSecondaryButton("Refresh History", onClick = {scope.launch{state.refreshHistory()}})}
            HistoryView.FILTERS->{Text("Stream Status",color=AppText,fontWeight=FontWeight.Bold);Spacer(Modifier.height(9.dp));SegmentRow(listOf("All","ended","failed","live"),filter){filter=it};Spacer(Modifier.height(16.dp));Text("Filter your real stream history by status. Additional platform and date filters can be applied as data becomes available.",color=AppTextSecondary,fontSize=12.sp,lineHeight=18.sp);Spacer(Modifier.height(12.dp));UlPrimaryButton("Apply Filters", onClick = {scope.launch {
                val remote = state.api.streamHistoryFiltered(filter.takeUnless { it == "All" }, 50, 0).getOrNull()
                if (remote != null) { /* local state remains source for screen; refresh keeps canonical ordering */ state.refreshHistory() }
            }; view=HistoryView.SESSIONS})}
            HistoryView.SESSIONS->{HistoryList(filtered,onOpenDetails);if(filtered.isEmpty())StudioCard{Text("No sessions match this filter.",color=AppTextSecondary)}}
            HistoryView.SEARCH->{UlTextField(query, { query = it }, "Search", placeholder = "Stream title...");Spacer(Modifier.height(14.dp));HistoryList(filtered,onOpenDetails)}
            HistoryView.HIGHLIGHTS->{val longest=state.streamHistory.maxByOrNull{it.durationSeconds?:0};val best=state.streamHistory.maxByOrNull{it.avgBitrateKbps?:0};listOf("Latest Stream" to state.streamHistory.firstOrNull(),"Longest Stream" to longest,"Best Technical Session" to best).forEach{(label,item)->StudioCard(onClick={item?.let{onOpenDetails(it.id)}}){Text(label,color=AppPrimary,fontWeight=FontWeight.Bold);Spacer(Modifier.height(4.dp));Text(item?.title?:"No data yet",color=AppText,fontWeight=FontWeight.Bold);Text(item?.let{historyMeta(it)}?:"Complete a stream to generate highlights.",color=AppTextSecondary,fontSize=11.sp)};Spacer(Modifier.height(8.dp))}}
        }
        Spacer(Modifier.height(18.dp));UlSecondaryButton("Back to Home", onClick = {onDestination(AppDestination.Home)})
    }
}

@Composable
private fun HistoryList(items:List<StreamHistoryItem>,onOpenDetails:(String)->Unit){
    items.forEach{item->StudioCard(onClick={onOpenDetails(item.id)}){Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(AppPrimary.copy(alpha=.10f)),contentAlignment=Alignment.Center){Text("▶",color=AppPrimary)};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(item.title?:"Untitled Stream",color=AppText,fontWeight=FontWeight.Bold,maxLines=1,overflow=TextOverflow.Ellipsis);Text(historyMeta(item),color=AppTextSecondary,fontSize=11.sp)};Text("›",color=AppTextMuted,fontSize=24.sp)}};Spacer(Modifier.height(8.dp))}
}
private fun historyMeta(item:StreamHistoryItem):String{val d=item.durationSeconds?:0;val mins=d/60;val secs=d%60;return "${item.status.ifBlank{"session"}} · ${mins}m ${secs}s · ${item.avgBitrateKbps?:0} Kbps"}

private enum class AnalyticsView { SUMMARY, PERFORMANCE, HEALTH, PLATFORM, ISSUES, INSIGHTS }

@Composable
fun Phase25StreamAnalyticsScreen(
    state: MobileIntegrationState,
    captureController: CaptureController,
    onBack: () -> Unit,
) {
    var view by remember { mutableStateOf(AnalyticsView.SUMMARY) }
    val item=state.selectedHistoryItem ?: state.streamHistory.firstOrNull()
    val analytics=state.selectedStreamAnalytics
    val snap=captureController.snapshot
    LaunchedEffect(item?.id) { if (item != null) state.refreshSelectedHistoryAnalytics() }

    LockedStudioPage(
        phase="STREAM ANALYTICS",
        title=when(view){AnalyticsView.SUMMARY->"Stream Details";AnalyticsView.PERFORMANCE->"Performance Metrics";AnalyticsView.HEALTH->"Stream Health";AnalyticsView.PLATFORM->"Platform Breakdown";AnalyticsView.ISSUES->"Issues Timeline";AnalyticsView.INSIGHTS->"Key Insights"},
        subtitle="Review stream health, bitrate, duration and destination performance from recorded telemetry.",
        onBack={if(view==AnalyticsView.SUMMARY)onBack()else view=AnalyticsView.SUMMARY},
    ) {
        SegmentRow(listOf("Summary","Performance","Health","Platforms","Issues","Insights"),when(view){AnalyticsView.SUMMARY->"Summary";AnalyticsView.PERFORMANCE->"Performance";AnalyticsView.HEALTH->"Health";AnalyticsView.PLATFORM->"Platforms";AnalyticsView.ISSUES->"Issues";AnalyticsView.INSIGHTS->"Insights"}){view=when(it){"Performance"->AnalyticsView.PERFORMANCE;"Health"->AnalyticsView.HEALTH;"Platforms"->AnalyticsView.PLATFORM;"Issues"->AnalyticsView.ISSUES;"Insights"->AnalyticsView.INSIGHTS;else->AnalyticsView.SUMMARY}}
        Spacer(Modifier.height(18.dp))
        if(item==null){StudioCard{Text("No stream selected. Open a session from Stream History first.",color=AppTextSecondary)};Spacer(Modifier.height(12.dp));UlSecondaryButton("Back",onBack);return@LockedStudioPage}
        when(view){
            AnalyticsView.SUMMARY->{PreviewCanvas(item.title?:"Stream Session",historyMeta(item),"${item.status.uppercase()}");Spacer(Modifier.height(12.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){MetricTile("Duration",formatDuration(item.durationSeconds),modifier=Modifier.weight(1f));MetricTile("Avg bitrate","${analytics?.avgBitrateKbps ?: item.avgBitrateKbps ?: 0} Kbps",modifier=Modifier.weight(1f))};Spacer(Modifier.height(8.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){MetricTile("Avg FPS",item.avgFps?.let{"%.1f".format(it)}?:"—",modifier=Modifier.weight(1f));MetricTile("Dropped",(analytics?.droppedFrames ?: item.droppedFrames).toString(),if((analytics?.droppedFrames ?: item.droppedFrames)==0)AppSuccess else AppWarning,Modifier.weight(1f))}}
            AnalyticsView.PERFORMANCE->{StudioCard{Text("Technical performance",color=AppText,fontWeight=FontWeight.Bold);Spacer(Modifier.height(12.dp));SettingRow("◌","Average FPS",item.avgFps?.let{"%.1f".format(it)}?:"No telemetry");SettingRow("↗","Average bitrate","${item.avgBitrateKbps?:0} Kbps");SettingRow("⚠","Dropped frames",item.droppedFrames.toString(),valueColor=if(item.droppedFrames==0)AppSuccess else AppWarning);SettingRow("⚙","Encoder",snap.encoderName.ifBlank{"Session telemetry"})};Spacer(Modifier.height(12.dp));Text("Performance values come from persisted stream telemetry for this broadcast.",color=AppTextSecondary,fontSize=11.sp,lineHeight=16.sp)}
            AnalyticsView.HEALTH->{val healthy=item.droppedFrames<20;StudioCard{HeaderStatus(if(healthy)"Stream Healthy" else "Review Stability",if(healthy)AppSuccess else AppWarning);Spacer(Modifier.height(12.dp));Text(if(healthy)"No major frame-loss signal was recorded in this summary." else "Dropped frames suggest the stream experienced pressure during the session.",color=AppTextSecondary,fontSize=12.sp,lineHeight=18.sp);Spacer(Modifier.height(14.dp));LinearProgressIndicator(progress={if(healthy).92f else .62f},modifier=Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),color=if(healthy)AppSuccess else AppWarning,trackColor=AppSurfaceInteractive)}}
            AnalyticsView.PLATFORM->{StudioCard{Text("Platform breakdown",color=AppText,fontWeight=FontWeight.Bold);Spacer(Modifier.height(10.dp));Text("Viewer and engagement totals appear only when the connected destination reports verified analytics.",color=AppTextSecondary,fontSize=12.sp,lineHeight=18.sp)};Spacer(Modifier.height(10.dp));listOf("YouTube","Facebook","Twitch","TikTok","Custom RTMP").forEach{p->StudioCard{Row(verticalAlignment=Alignment.CenterVertically){Text("●",color=AppTextMuted);Spacer(Modifier.width(10.dp));Text(p,color=AppText,modifier=Modifier.weight(1f));Text("No analytics yet",color=AppTextMuted,fontSize=11.sp)}};Spacer(Modifier.height(7.dp))}}
            AnalyticsView.ISSUES->{val issues=mutableListOf<String>();if(item.droppedFrames>0)issues+="${item.droppedFrames} dropped frames";if((item.avgBitrateKbps?:0)<3000)issues+="Average bitrate below high-quality target";if(issues.isEmpty())issues+="No summary-level issues detected";issues.forEachIndexed{index,issue->StudioCard{Row{Box(Modifier.size(34.dp).clip(CircleShape).background(if(issue.startsWith("No"))AppSuccess.copy(alpha=.15f) else AppWarning.copy(alpha=.15f)),contentAlignment=Alignment.Center){Text(if(issue.startsWith("No"))"✓" else "!",color=if(issue.startsWith("No"))AppSuccess else AppWarning,fontWeight=FontWeight.Black)};Spacer(Modifier.width(12.dp));Column{Text(if(index==0)"Session check" else "Technical note",color=AppText,fontWeight=FontWeight.Bold);Text(issue,color=AppTextSecondary,fontSize=11.sp)}}};Spacer(Modifier.height(8.dp))}}
            AnalyticsView.INSIGHTS->{val fps=item.avgFps?:0.0;val bitrate=item.avgBitrateKbps?:0;val insights=listOf(if(item.droppedFrames==0)"Technical Excellence" to "No dropped frames were recorded in the persisted summary." else "Stability Opportunity" to "Reduce load or bitrate if dropped frames repeat.",if(fps>=29)"Frame Rate On Target" to "Average FPS stayed near the configured 30 FPS baseline." else "FPS Opportunity" to "Review device load and capture settings.",if(bitrate>=6000)"High Quality Bitrate" to "Average bitrate supported a strong 1080p profile." else "Bandwidth Opportunity" to "Use the quality presets to better match available upload bandwidth.");insights.forEach{(a,b)->StudioCard{Text(a,color=AppPrimary,fontWeight=FontWeight.Bold);Text(b,color=AppTextSecondary,fontSize=12.sp,lineHeight=17.sp)};Spacer(Modifier.height(8.dp))}}
        }
    }
}

private fun formatDuration(seconds: Int?): String {
    if (seconds == null) return "—"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m ${s}s"
}
