package com.universallive.app.streaming.overlays

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class OverlayKind { TEXT, IMAGE, LOGO }

data class OverlayLayer(
    val id: String,
    val kind: OverlayKind,
    val label: String,
    val enabled: Boolean = true,
    val x: Float = 0.04f,
    val y: Float = 0.04f,
    val width: Float = 0.22f,
    val opacity: Float = 1f,
    val text: String = "",
    val assetPath: String = "",
)

class OverlayState {
    // No forced broadcast watermark. User-added overlays only.
    var layers by mutableStateOf<List<OverlayLayer>>(emptyList()); private set

    fun replaceFromBackend(next: List<OverlayLayer>) { layers = next }

    fun upsert(layer: OverlayLayer) {
        val old = layers.indexOfFirst { it.id == layer.id }
        layers = if (old < 0) layers + layer else layers.toMutableList().also { it[old] = layer }
    }
    fun remove(id: String) { layers = layers.filterNot { it.id == id } }
    fun move(id: String, delta: Int) {
        val from = layers.indexOfFirst { it.id == id }; if (from < 0) return
        val to = (from + delta).coerceIn(0, layers.lastIndex); if (to == from) return
        val next = layers.toMutableList(); val item = next.removeAt(from); next.add(to, item); layers = next
    }
    fun clear() { layers = emptyList() }
}
