package com.universallive.app.streaming.health

enum class StreamHealthGrade { EXCELLENT, GOOD, DEGRADED, POOR, OFFLINE }

data class StreamHealthSnapshot(
    val grade: StreamHealthGrade = StreamHealthGrade.OFFLINE,
    val networkBitrateBps: Long = 0,
    val targetBitrateKbps: Int = 0,
    val estimatedDroppedFrames: Long = 0,
    val reconnectCount: Int = 0,
    val adaptiveBitrateEnabled: Boolean = true,
    val effectiveBitrateKbps: Int = 0,
) {
    val utilization: Float
        get() = if (targetBitrateKbps <= 0) 0f else (networkBitrateBps / 1000f / targetBitrateKbps).coerceIn(0f, 2f)
}

data class RecoveryPolicy(
    val maxReconnectAttempts: Int = 8,
    val reconnectDelayMs: Long = 1500,
    val maxReconnectDelayMs: Long = 12_000,
    val keepCaptureAliveDuringReconnect: Boolean = true,
    val requestKeyFrameAfterReconnect: Boolean = true,
)
