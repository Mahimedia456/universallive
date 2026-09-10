package com.universallive.app.streaming.platform

/** Shared streaming contract for Android MediaProjection/MediaCodec and iOS ReplayKit/VideoToolbox. */
interface PlatformStreamingEngine {
    val platformName: String
    fun supportsScreenCapture(): Boolean
    fun supportsInternalAudio(): Boolean
    fun supportsFacecamComposition(): Boolean
    fun supportsRtmp(): Boolean
}

data class PlatformCapabilities(
    val screenCapture: Boolean,
    val internalAudio: Boolean,
    val facecamComposition: Boolean,
    val rtmp: Boolean,
)
