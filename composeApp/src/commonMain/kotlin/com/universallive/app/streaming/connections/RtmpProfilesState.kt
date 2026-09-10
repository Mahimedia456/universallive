package com.universallive.app.streaming.connections

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class RtmpProfilesState {
    var profiles by mutableStateOf(
        listOf(
            RtmpProfile(
                id = 1,
                name = "YouTube Main",
                platform = StreamPlatform.YouTube,
                serverUrl = StreamPlatform.YouTube.defaultServerUrl,
                streamKey = "",
            ),
        )
    )
        private set

    val activeProfiles: List<RtmpProfile>
        get() = profiles.filter { it.enabled && it.isValid && it.streamKey.isNotBlank() }

    fun save(profile: RtmpProfile) {
        profiles = if (profiles.any { it.id == profile.id }) {
            profiles.map { if (it.id == profile.id) profile else it }
        } else {
            profiles + profile.copy(id = nextId())
        }
    }

    fun toggle(id: Long) {
        profiles = profiles.map { if (it.id == id) it.copy(enabled = !it.enabled) else it }
    }

    fun delete(id: Long) {
        profiles = profiles.filterNot { it.id == id }
    }

    private fun nextId(): Long = (profiles.maxOfOrNull { it.id } ?: 0L) + 1L
}
