package com.universallive.app.streaming.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.universallive.app.streaming.model.StreamConfig
import com.universallive.app.streaming.model.StreamFps
import com.universallive.app.streaming.model.StreamOrientation
import com.universallive.app.streaming.model.StreamResolution

class StreamConfigState(initial: StreamConfig = StreamConfig()) {
    var config by mutableStateOf(initial)
        private set

    fun setResolution(value: StreamResolution) {
        config = config.copy(resolution = value)
    }

    fun setFps(value: StreamFps) {
        config = config.copy(fps = value)
    }

    fun setBitrateKbps(value: Int) {
        config = config.copy(bitrateKbps = value)
    }

    fun setOrientation(value: StreamOrientation) {
        config = config.copy(orientation = value)
    }

    fun setMicrophoneEnabled(value: Boolean) {
        config = config.copy(microphoneEnabled = value)
    }

    fun setInternalAudioEnabled(value: Boolean) {
        config = config.copy(internalAudioEnabled = value)
    }

    fun reset() {
        config = StreamConfig()
    }
}
