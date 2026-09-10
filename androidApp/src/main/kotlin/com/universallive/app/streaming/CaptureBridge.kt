package com.universallive.app.streaming

import com.universallive.app.streaming.capture.CaptureSnapshot

object CaptureBridge {
    @Volatile var latest: CaptureSnapshot? = null
        private set

    var listener: ((CaptureSnapshot) -> Unit)? = null
        set(value) {
            field = value
            latest?.let { value?.invoke(it) }
        }

    fun publish(snapshot: CaptureSnapshot) {
        latest = snapshot
        listener?.invoke(snapshot)
    }
}
