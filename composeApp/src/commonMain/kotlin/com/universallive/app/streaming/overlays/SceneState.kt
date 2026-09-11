package com.universallive.app.streaming.overlays

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class StreamScene(
    val id: String,
    val name: String,
    val layerIds: List<String>,
)

class SceneState {
    var scenes by mutableStateOf(listOf(StreamScene("scene-main", "Main", emptyList()))); private set
    var activeSceneId by mutableStateOf("scene-main"); private set
    val activeScene get() = scenes.firstOrNull { it.id == activeSceneId } ?: scenes.first()

    fun replaceFromBackend(next: List<StreamScene>, activeId: String?) {
        if (next.isEmpty()) return
        scenes = next
        activeSceneId = activeId?.takeIf { id -> next.any { it.id == id } } ?: next.first().id
    }

    fun add(name: String, layerIds: List<String>) {
        val id = "scene-${scenes.size + 1}"
        scenes = scenes + StreamScene(id, name.ifBlank { "Scene ${scenes.size + 1}" }, layerIds)
        activeSceneId = id
    }
    fun activate(id: String) { if (scenes.any { it.id == id }) activeSceneId = id }
    fun delete(id: String) {
        if (scenes.size <= 1) return
        scenes = scenes.filterNot { it.id == id }
        if (activeSceneId == id) activeSceneId = scenes.first().id
    }
}
