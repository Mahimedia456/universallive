package com.universallive.app.data.live

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StreamConfigDto(
    val id: String,
    val name: String,
    val resolution: String,
    val width: Int,
    val height: Int,
    val fps: Int,
    @SerialName("bitrate_kbps")
    val bitrateKbps: Int,
    val orientation: String,
    @SerialName("microphone_enabled")
    val microphoneEnabled: Boolean,
    @SerialName("internal_audio_enabled")
    val internalAudioEnabled: Boolean,
    @SerialName("facecam_enabled")
    val facecamEnabled: Boolean,
    @SerialName("scene_id")
    val sceneId: String? = null,
    @SerialName("connection_id")
    val connectionId: String? = null,
    val privacy: String = "public",
)

@Serializable
data class BroadcastDestinationRequest(
    val connectionId: String? = null,
    val platform: String,
)

@Serializable
data class CreateBroadcastSessionRequest(
    val title: String? = null,
    val description: String? = null,
    val sceneId: String? = null,
    val streamConfigId: String? = null,
    val clientSessionId: String? = null,
    val destinations: List<BroadcastDestinationRequest> = emptyList(),
)

@Serializable
data class BroadcastSessionDto(
    val id: String,
    val title: String? = null,
    val description: String? = null,
    val status: String,
    @SerialName("scene_id")
    val sceneId: String? = null,
    @SerialName("stream_config_id")
    val streamConfigId: String? = null,
    @SerialName("started_at")
    val startedAt: String? = null,
    @SerialName("ended_at")
    val endedAt: String? = null,
    @SerialName("last_heartbeat_at")
    val lastHeartbeatAt: String? = null,
)

@Serializable
data class StreamTelemetryRequest(
    val bitrateKbps: Int? = null,
    val targetBitrateKbps: Int? = null,
    val fps: Double? = null,
    val droppedFrames: Int? = null,
    val publishedVideoFrames: Long? = null,
    val publishedAudioFrames: Long? = null,
    val encoderWidth: Int? = null,
    val encoderHeight: Int? = null,
    val encoderName: String? = null,
    val networkStatus: String? = null,
    val publishStatus: String? = null,
    val audioStatus: String? = null,
    val thermalState: String? = null,
    val batteryPercent: Int? = null,
    val metadata: Map<String, String> = emptyMap(),
)

@Serializable
data class StreamSummaryDto(
    @SerialName("session_id")
    val sessionId: String,
    @SerialName("duration_seconds")
    val durationSeconds: Int? = null,
    @SerialName("avg_bitrate_kbps")
    val avgBitrateKbps: Int? = null,
    @SerialName("peak_bitrate_kbps")
    val peakBitrateKbps: Int? = null,
    @SerialName("avg_fps")
    val avgFps: Double? = null,
    @SerialName("dropped_frames")
    val droppedFrames: Int = 0,
    @SerialName("reconnect_count")
    val reconnectCount: Int = 0,
    @SerialName("total_video_frames")
    val totalVideoFrames: Long = 0,
    @SerialName("total_audio_frames")
    val totalAudioFrames: Long = 0,
    @SerialName("destination_count")
    val destinationCount: Int = 0,
    @SerialName("successful_destination_count")
    val successfulDestinationCount: Int = 0,
    @SerialName("failed_destination_count")
    val failedDestinationCount: Int = 0,
    @SerialName("final_status")
    val finalStatus: String? = null,
)
