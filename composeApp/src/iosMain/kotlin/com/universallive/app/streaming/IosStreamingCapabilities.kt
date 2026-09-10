package com.universallive.app.streaming

import com.universallive.app.streaming.platform.PlatformStreamingEngine

/** Phase 17 abstraction. Concrete ReplayKit/AVFoundation transport is added in the iOS native phase. */
class IosStreamingCapabilities : PlatformStreamingEngine {
    override val platformName: String = "iOS"
    override fun supportsScreenCapture() = true
    override fun supportsInternalAudio() = true
    override fun supportsFacecamComposition() = true
    override fun supportsRtmp() = true
}
