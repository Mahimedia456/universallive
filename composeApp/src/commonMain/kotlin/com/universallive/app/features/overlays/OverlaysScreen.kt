package com.universallive.app.features.overlays

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
import com.universallive.app.streaming.overlays.*
import com.universallive.app.theme.*

@Composable
fun OverlaysScreen(state: OverlayState, onDestinationChanged: (AppDestination)->Unit) {
    var text by remember { mutableStateOf("Stream starting soon") }
    AppScaffold("Overlays", AppDestination.Overlays, onDestinationChanged) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Scene layers", color=AppText)
            state.layers.forEach { layer ->
                Column(Modifier.fillMaxWidth().background(AppSurface, RoundedCornerShape(16.dp)).border(1.dp, AppBorder, RoundedCornerShape(16.dp)).padding(14.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) { Text(layer.label, color=AppText); Text(layer.kind.name, color=AppTextMuted) }
                        Switch(layer.enabled, { state.upsert(layer.copy(enabled=it)) })
                    }
                    Slider(layer.width, { state.upsert(layer.copy(width=it)) }, valueRange=.10f..1f)
                    Slider(layer.opacity, { state.upsert(layer.copy(opacity=it)) }, valueRange=.1f..1f)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ state.move(layer.id,-1) }) { Text("UP") }
                        OutlinedButton({ state.move(layer.id,1) }) { Text("DOWN") }
                        OutlinedButton({ state.remove(layer.id) }) { Text("DELETE") }
                    }
                }
            }
            OutlinedTextField(text, { text=it }, label={Text("New text overlay")}, modifier=Modifier.fillMaxWidth())
            Button({
                val id="text-${state.layers.size+1}"
                state.upsert(OverlayLayer(id, OverlayKind.TEXT, text.ifBlank { "Text" }, text=text.ifBlank { "LIVE" }, x=.08f, y=.78f, width=.36f))
                text=""
            }, modifier=Modifier.fillMaxWidth()) { Text("ADD TEXT LAYER") }
            Text("Image/logo layers accept a device-local asset path in the scene payload; missing files are skipped by the native compositor instead of stopping the stream.", color=AppTextMuted)
            Spacer(Modifier.height(12.dp))
        }
    }
}
