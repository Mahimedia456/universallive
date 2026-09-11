package com.universallive.app.streaming.capture

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class CaptureStatus { IDLE, REQUESTING_PERMISSION, STARTING, CAPTURING, STOPPING, ERROR }
enum class PublishStatus { IDLE, CONNECTING, LIVE, RECONNECTING, ERROR, DISCONNECTED }
enum class CaptureMode(val label: String) { ENTIRE_DEVICE("Entire device"), USER_CHOICE("Choose app / screen") }

data class CaptureSnapshot(
    val status: CaptureStatus = CaptureStatus.IDLE,
    val message: String = "Screen capture is idle",
    val framesObserved: Long = 0,
    val encodedVideoFrames: Long = 0,
    val encodedVideoBytes: Long = 0,
    val encoderName: String = "",
    val encoderWidth: Int = 0,
    val encoderHeight: Int = 0,
    val encoderFps: Int = 0,
    val encoderBitrateKbps: Int = 0,
    val microphoneRequested: Boolean = false,
    val internalAudioRequested: Boolean = false,
    val microphoneActive: Boolean = false,
    val internalAudioActive: Boolean = false,
    val microphoneBytes: Long = 0,
    val internalAudioBytes: Long = 0,
    val microphoneLevel: Float = 0f,
    val internalAudioLevel: Float = 0f,
    val audioEncoderName: String = "",
    val encodedAudioFrames: Long = 0,
    val encodedAudioBytes: Long = 0,
    val publishedAudioFrames: Long = 0,
    val publishedAudioBytes: Long = 0,
    val audioMessage: String = "Audio capture is idle",
    val publishStatus: PublishStatus = PublishStatus.IDLE,
    val publishMessage: String = "RTMP publisher is idle",
    val publishTarget: String = "",
    val publishedVideoFrames: Long = 0,
    val publishedVideoBytes: Long = 0,
    val networkBitrateBps: Long = 0,
    val encoderMeasuredBitrateKbps: Int = 0,
    val encodedFpsActual: Double = 0.0,
    val sentFpsActual: Double = 0.0,
    val droppedFrames: Int? = null,
    val rtmpQueueDepth: Int? = null,
    val socketWriteLatencyMs: Int? = null,
    val publisherEnqueueLatencyMs: Int? = null,
    val lastVideoPacketAgeMs: Int? = null,
    val lastAudioPacketAgeMs: Int? = null,
    val keyframeIntervalMs: Int? = null,
    val videoPtsMonotonic: Boolean? = null,
    val audioPtsMonotonic: Boolean? = null,
    val reconnectCount: Int = 0,
    val publisherInstanceId: String = "",
    val facecamActive: Boolean = false,
    val compositorActive: Boolean = false,
    val activeSceneName: String = "Main",
    val captureMode: String = "Entire device",
    val startedAtEpochMs: Long = 0L,
) {
    val isActive: Boolean get() = status in setOf(CaptureStatus.REQUESTING_PERMISSION, CaptureStatus.STARTING, CaptureStatus.CAPTURING, CaptureStatus.STOPPING)
    val isPublishing: Boolean get() = publishStatus in setOf(PublishStatus.CONNECTING, PublishStatus.LIVE, PublishStatus.RECONNECTING)
}

class CaptureController(
    private val requestStart: () -> Unit = {},
    private val requestStop: () -> Unit = {},
    private val requestSceneUpdate: (String, String) -> Unit = { _, _ -> },
    private val requestBitrateUpdate: (Int) -> Unit = {},
) {
    var snapshot by mutableStateOf(CaptureSnapshot()); private set
    var requestedMicrophone: Boolean = true; private set
    var requestedInternalAudio: Boolean = true; private set
    var requestedWidth: Int = 1920; private set
    var requestedHeight: Int = 1080; private set
    var requestedFps: Int = 30; private set
    var requestedBitrateKbps: Int = 6800; private set
    var requestedOrientation: String = "Landscape"; private set
    var requestedRtmpServerUrl: String = ""; private set
    var requestedStreamKey: String = ""; private set
    var requestedTargetName: String = ""; private set
    var requestedFacecamEnabled: Boolean = false; private set
    var requestedFacecamLens: String = "Front"; private set
    var requestedFacecamShape: String = "Circle"; private set
    var requestedFacecamX: Float = 0.72f; private set
    var requestedFacecamY: Float = 0.08f; private set
    var requestedFacecamSize: Float = 0.24f; private set
    var requestedFacecamMirrored: Boolean = true; private set
    var requestedOverlayPayload: String = ""; private set
    var requestedSceneName: String = "Main"; private set
    var adaptiveBitrateEnabled: Boolean = true; private set
    var requestedCaptureMode: CaptureMode = CaptureMode.ENTIRE_DEVICE; private set

    fun configureAudio(microphone: Boolean, internalAudio: Boolean) { requestedMicrophone = microphone; requestedInternalAudio = internalAudio }
    fun setCaptureMode(mode: CaptureMode) { requestedCaptureMode = mode }
    fun configureVideo(width: Int, height: Int, fps: Int, bitrateKbps: Int, orientation: String) {
        requestedWidth = width; requestedHeight = height; requestedFps = fps; requestedBitrateKbps = bitrateKbps; requestedOrientation = orientation
    }
    fun configurePublish(serverUrl: String, streamKey: String, targetName: String) {
        requestedRtmpServerUrl = serverUrl.trim(); requestedStreamKey = streamKey.trim(); requestedTargetName = targetName.trim()
    }
    fun configureFacecam(enabled: Boolean, lens: String, shape: String, x: Float, y: Float, size: Float, mirrored: Boolean) {
        requestedFacecamEnabled = enabled; requestedFacecamLens = lens; requestedFacecamShape = shape
        requestedFacecamX = x; requestedFacecamY = y; requestedFacecamSize = size; requestedFacecamMirrored = mirrored
    }
    fun configureOverlays(sceneName: String, payload: String) { requestedSceneName = sceneName; requestedOverlayPayload = payload }
    fun updateSceneLive(sceneName: String, payload: String) {
        configureOverlays(sceneName, payload)
        if (snapshot.status == CaptureStatus.CAPTURING) requestSceneUpdate(sceneName, payload)
    }
    fun setAdaptiveBitrateEnabled(enabled: Boolean) { adaptiveBitrateEnabled = enabled }
    fun updateBitrateLive(kbps: Int) {
        requestedBitrateKbps = kbps.coerceAtLeast(500)
        if (snapshot.status == CaptureStatus.CAPTURING) requestBitrateUpdate(requestedBitrateKbps)
    }
    fun start() {
        if (snapshot.isActive) return
        snapshot = CaptureSnapshot(
            status = CaptureStatus.REQUESTING_PERMISSION,
            message = "Waiting for Android capture permissions",
            microphoneRequested = requestedMicrophone,
            internalAudioRequested = requestedInternalAudio,
            encoderWidth = requestedWidth, encoderHeight = requestedHeight, encoderFps = requestedFps, encoderBitrateKbps = requestedBitrateKbps,
            audioMessage = if (requestedMicrophone || requestedInternalAudio) "Preparing audio capture" else "Audio capture disabled",
            publishStatus = if (requestedRtmpServerUrl.isNotBlank() && requestedStreamKey.isNotBlank()) PublishStatus.CONNECTING else PublishStatus.IDLE,
            publishMessage = if (requestedRtmpServerUrl.isNotBlank() && requestedStreamKey.isNotBlank()) "RTMP destination configured" else "No active RTMP destination",
            publishTarget = requestedTargetName,
            captureMode = requestedCaptureMode.label,
        )
        requestStart()
    }
    fun stop() { if (!snapshot.isActive && snapshot.status != CaptureStatus.ERROR) return; snapshot = snapshot.copy(status = CaptureStatus.STOPPING, message = "Stopping screen/audio/RTMP pipeline"); requestStop() }
    fun update(next: CaptureSnapshot) { snapshot = next }
    fun permissionDenied(message: String = "Capture permission was not granted") {
        snapshot = CaptureSnapshot(status = CaptureStatus.IDLE, message = message, microphoneRequested = requestedMicrophone, internalAudioRequested = requestedInternalAudio, encoderWidth=requestedWidth, encoderHeight=requestedHeight, encoderFps=requestedFps, encoderBitrateKbps=requestedBitrateKbps, audioMessage="Audio capture did not start")
    }
}
