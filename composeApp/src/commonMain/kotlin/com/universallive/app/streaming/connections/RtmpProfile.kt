package com.universallive.app.streaming.connections

enum class StreamPlatform(
    val label: String,
    val hint: String,
    val defaultServerUrl: String,
) {
    YouTube(
        label = "YouTube",
        hint = "Paste the RTMP/RTMPS server URL and stream key from YouTube Live Control Room.",
        defaultServerUrl = "rtmps://a.rtmps.youtube.com/live2",
    ),
    Facebook(
        label = "Facebook",
        hint = "Use the server URL and stream key shown by Facebook Live Producer.",
        defaultServerUrl = "rtmps://live-api-s.facebook.com:443/rtmp/",
    ),
    TikTok(
        label = "TikTok",
        hint = "Available only when your TikTok account provides encoder/stream-key access.",
        defaultServerUrl = "",
    ),
    Custom(
        label = "Custom RTMP",
        hint = "Connect to any compatible RTMP or RTMPS ingest server.",
        defaultServerUrl = "",
    ),
}

data class RtmpProfile(
    val id: Long,
    val name: String,
    val platform: StreamPlatform,
    val serverUrl: String,
    val streamKey: String,
    val enabled: Boolean = true,
) {
    val maskedKey: String
        get() = when {
            streamKey.isBlank() -> "Not set"
            streamKey.length <= 6 -> "••••••"
            else -> "${streamKey.take(3)}••••••${streamKey.takeLast(3)}"
        }

    val isValid: Boolean
        get() = serverUrl.startsWith("rtmp://") || serverUrl.startsWith("rtmps://")
}
