package com.universallive.app.features.scenes

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.universallive.app.components.AppScaffold
import com.universallive.app.navigation.AppDestination
import com.universallive.app.streaming.capture.CaptureController
import com.universallive.app.streaming.overlays.*
import com.universallive.app.theme.*

private fun scenePayload(scene: StreamScene, overlayState: OverlayState): String {
    val activeIds = scene.layerIds.toSet()
    return overlayState.layers.filter { it.id in activeIds && it.enabled }.joinToString("§") { layer ->
        listOf(layer.kind.name, layer.x, layer.y, layer.width, layer.opacity, layer.text, layer.assetPath)
            .joinToString("¦") { it.toString().replace("§", " ").replace("¦", " ") }
    }
}

@Composable
fun ScenesScreen(sceneState: SceneState, overlayState: OverlayState, captureController: CaptureController, onDestinationChanged:(AppDestination)->Unit) {
    var name by remember { mutableStateOf("") }
    AppScaffold("Scenes", AppDestination.Scenes, onDestinationChanged) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if (captureController.snapshot.status.name == "CAPTURING") {
                Text("LIVE SCENE SWITCHING ACTIVE", color=AppLive)
                Text("Changing scenes updates the OpenGL compositor without restarting capture, audio, encoder or RTMP.", color=AppTextMuted)
            }
            sceneState.scenes.forEach { scene ->
                val active = scene.id == sceneState.activeSceneId
                Row(Modifier.fillMaxWidth().background(if(active) AppSurfaceRaised else AppSurface, RoundedCornerShape(16.dp)).border(1.dp, if(active) AppPrimary else AppBorder, RoundedCornerShape(16.dp)).padding(14.dp)) {
                    Column(Modifier.weight(1f)) { Text(scene.name, color=AppText); Text("${scene.layerIds.size} layers", color=AppTextMuted) }
                    TextButton({
                        sceneState.activate(scene.id)
                        captureController.updateSceneLive(scene.name, scenePayload(scene, overlayState))
                    }) { Text(if(active) "ACTIVE" else if(captureController.snapshot.status.name == "CAPTURING") "TAKE LIVE" else "USE") }
                    if (sceneState.scenes.size > 1) TextButton({ sceneState.delete(scene.id) }) { Text("DELETE") }
                }
            }
            OutlinedTextField(name, {name=it}, label={Text("Scene name")}, modifier=Modifier.fillMaxWidth())
            Button({ sceneState.add(name, overlayState.layers.map { it.id }); name="" }, modifier=Modifier.fillMaxWidth()) { Text("SAVE CURRENT LAYOUT AS SCENE") }
            Text("Switch scenes while live; the active native compositor applies the selected layout.", color=AppTextMuted)
        }
    }
}
