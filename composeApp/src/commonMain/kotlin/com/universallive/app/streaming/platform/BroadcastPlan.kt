package com.universallive.app.streaming.platform

import com.universallive.app.streaming.connections.RtmpProfile

enum class BroadcastMode {
    SINGLE_DESTINATION,
    DIRECT_MULTI_DESTINATION,
    RELAY_MULTI_DESTINATION,
}

data class BroadcastTarget(
    val profileId: Long,
    val label: String,
    val serverUrl: String,
    val streamKey: String,
)

data class BroadcastPlan(
    val mode: BroadcastMode = BroadcastMode.SINGLE_DESTINATION,
    val targets: List<BroadcastTarget> = emptyList(),
) {
    val primary: BroadcastTarget? get() = targets.firstOrNull()
    val isReady: Boolean get() = targets.isNotEmpty() && targets.all { it.serverUrl.isNotBlank() && it.streamKey.isNotBlank() }

    companion object {
        fun fromProfiles(profiles: List<RtmpProfile>, mode: BroadcastMode): BroadcastPlan {
            val active = profiles.filter { it.enabled && it.isValid && it.streamKey.isNotBlank() }
            val selected = when (mode) {
                BroadcastMode.SINGLE_DESTINATION -> active.take(1)
                BroadcastMode.DIRECT_MULTI_DESTINATION,
                BroadcastMode.RELAY_MULTI_DESTINATION -> active
            }
            return BroadcastPlan(
                mode = mode,
                targets = selected.map {
                    BroadcastTarget(
                        profileId = it.id,
                        label = it.name,
                        serverUrl = it.serverUrl,
                        streamKey = it.streamKey,
                    )
                },
            )
        }
    }
}
