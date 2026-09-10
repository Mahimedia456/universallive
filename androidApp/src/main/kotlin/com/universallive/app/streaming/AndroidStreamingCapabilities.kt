package com.universallive.app.streaming

import android.os.Build
import com.universallive.app.streaming.platform.PlatformStreamingEngine

class AndroidStreamingCapabilities : PlatformStreamingEngine {
    override val platformName: String = "Android"
    override fun supportsScreenCapture() = true
    override fun supportsInternalAudio() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    override fun supportsFacecamComposition() = true
    override fun supportsRtmp() = true
}
