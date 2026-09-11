package com.universallive.app.streaming.model

enum class StreamResolution(val label: String, val width: Int, val height: Int) {
    P480("480p", 854, 480),
    P720("720p HD", 1280, 720),
    P1080("1080p Full HD", 1920, 1080),
}

enum class StreamFps(val value: Int) {
    FPS30(30),
    FPS60(60),
}

enum class StreamOrientation(val label: String) {
    Landscape("Landscape"),
    Portrait("Portrait"),
    Auto("Auto"),
}

data class StreamConfig(
    val resolution: StreamResolution = StreamResolution.P1080,
    val fps: StreamFps = StreamFps.FPS30,
    val bitrateKbps: Int = 6800,
    val orientation: StreamOrientation = StreamOrientation.Landscape,
    val microphoneEnabled: Boolean = true,
    val internalAudioEnabled: Boolean = true,
) {
    val bitrateLabel: String
        get() = if (bitrateKbps >= 1000 && bitrateKbps % 1000 == 0) {
            "${bitrateKbps / 1000} Mbps"
        } else {
            "${bitrateKbps} Kbps"
        }

    val videoSummary: String
        get() = "${resolution.label} • ${fps.value} FPS"
}

object StreamConfigLimits {
    val bitrateOptionsKbps = listOf(2000, 3500, 4500, 6000, 6800, 8000, 10000, 12000)
}
