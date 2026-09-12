package com.universallive.app.streaming

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.SystemClock
import com.pedro.common.ConnectChecker
import com.pedro.rtmp.rtmp.RtmpClient
import java.nio.ByteBuffer
import java.util.concurrent.locks.LockSupport

/**
 * RTMP publisher with two safeguards that are important for strict ingests such as YouTube:
 * 1) wait for H.264 SPS/PPS before opening RTMP, and
 * 2) rewrite/pacing outgoing media timestamps against Android's monotonic clock.
 *
 * Some MediaProjection/Surface encoder combinations can emit buffered frames with timestamps
 * that advance faster than wall time. Sending those timestamps unchanged makes YouTube report
 * "encoder is sending data faster than realtime" and can leave the public player black even
 * while the RTMP connection itself is LIVE.
 */
class RtmpPublisher(
    private val onState: (State) -> Unit,
) : ConnectChecker {
    enum class Status { IDLE, CONNECTING, LIVE, RECONNECTING, ERROR, DISCONNECTED }

    data class State(
        val status: Status = Status.IDLE,
        val message: String = "RTMP publisher is idle",
        val bitrateBps: Long = 0,
    )

    data class Metrics(
        val lastVideoPacketAgeMs: Int? = null,
        val lastAudioPacketAgeMs: Int? = null,
        val publisherEnqueueLatencyMs: Int? = null,
        val socketWriteLatencyMs: Int? = null,
        val rtmpQueueDepth: Int? = null,
        val keyframeIntervalMs: Int? = null,
        val videoPtsMonotonic: Boolean? = null,
        val audioPtsMonotonic: Boolean? = null,
        val reconnectCount: Int = 0,
    )

    private val client = RtmpClient(this)
    private val videoPaceLock = Any()
    private val audioPaceLock = Any()
    private val ptsLock = Any()

    @Volatile private var currentUrl: String = ""
    @Volatile private var stoppedByUser = false
    @Volatile private var startRequested = false
    @Volatile private var videoInfoReady = false
    @Volatile private var connectionStarted = false
    @Volatile private var awaitingFirstKeyframe = true
    @Volatile private var targetFps = 30
    @Volatile private var publishClockStartNs = 0L

    private var nextVideoSendNs = 0L
    private var lastVideoPtsUs = -1L
    private var lastAudioPtsUs = -1L
    private var firstAudioSourcePtsUs = -1L

    @Volatile private var lastVideoSentAtMs = 0L
    @Volatile private var lastAudioSentAtMs = 0L
    @Volatile private var lastPublisherEnqueueLatencyMs = -1
    @Volatile private var lastKeyframeSentAtMs = 0L
    @Volatile private var measuredKeyframeIntervalMs = -1
    @Volatile private var videoPtsMonotonic = true
    @Volatile private var audioPtsMonotonic = true
    @Volatile private var previousVideoPacketPtsUs = -1L
    @Volatile private var previousAudioPacketPtsUs = -1L
    @Volatile private var reconnectCount = 0

    fun start(url: String, width: Int, height: Int, fps: Int, audioEnabled: Boolean) {
        currentUrl = url
        stoppedByUser = false
        startRequested = true
        connectionStarted = false
        awaitingFirstKeyframe = true
        targetFps = fps.coerceIn(15, 60)
        reconnectCount = 0
        lastVideoSentAtMs = 0L
        lastAudioSentAtMs = 0L
        lastPublisherEnqueueLatencyMs = -1
        lastKeyframeSentAtMs = 0L
        measuredKeyframeIntervalMs = -1
        videoPtsMonotonic = true
        audioPtsMonotonic = true
        previousVideoPacketPtsUs = -1L
        previousAudioPacketPtsUs = -1L
        resetRealtimeClock()

        client.setOnlyVideo(!audioEnabled)
        if (audioEnabled) client.setAudioInfo(48_000, true)
        client.setVideoResolution(width, height)
        client.setFps(targetFps)
        client.setReTries(8)

        if (videoInfoReady) connectIfReady()
        else onState(State(Status.CONNECTING, "Waiting for H.264 SPS/PPS before opening ingest"))
    }

    fun setVideoFormat(format: MediaFormat) {
        val sps = format.getByteBuffer("csd-0")?.duplicate() ?: return
        val pps = format.getByteBuffer("csd-1")?.duplicate()
        client.setVideoInfo(sps, pps, null)
        videoInfoReady = true
        connectIfReady()
    }

    @Synchronized
    private fun connectIfReady() {
        if (!startRequested || !videoInfoReady || connectionStarted || currentUrl.isBlank() || stoppedByUser) return
        connectionStarted = true
        onState(State(Status.CONNECTING, "H.264 ready • opening RTMP/RTMPS ingest"))
        client.connect(currentUrl)
    }

    fun sendVideo(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        if (!client.isStreaming || info.size <= 0 || info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) return

        // Strict ingests must begin decoding from an IDR/keyframe. Do not let P/B frames race
        // ahead of the sync-frame request immediately after RTMP connect/reconnect. SPS/PPS is
        // already configured through setVideoInfo before client.connect().
        if (awaitingFirstKeyframe) {
            if (info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME == 0) return
            awaitingFirstKeyframe = false
            resetRealtimeClock()
        }

        // Keep upload cadence tied to wall clock. This prevents a buffered MediaCodec drain from
        // transmitting several seconds of video during one real second.
        val ptsUs = paceVideoAndGetPtsUs()

        val dup = buffer.duplicate()
        dup.position(info.offset)
        dup.limit(info.offset + info.size)
        val packetInfo = MediaCodec.BufferInfo().apply {
            set(0, info.size, ptsUs, info.flags)
        }
        if (previousVideoPacketPtsUs >= 0L && ptsUs <= previousVideoPacketPtsUs) videoPtsMonotonic = false
        previousVideoPacketPtsUs = ptsUs
        val beforeNs = SystemClock.elapsedRealtimeNanos()
        client.sendVideo(dup.slice(), packetInfo)
        val afterMs = SystemClock.elapsedRealtime()
        lastPublisherEnqueueLatencyMs = ((SystemClock.elapsedRealtimeNanos() - beforeNs) / 1_000_000L).toInt()
        lastVideoSentAtMs = afterMs
        if (info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0) {
            if (lastKeyframeSentAtMs > 0L) {
                measuredKeyframeIntervalMs = (afterMs - lastKeyframeSentAtMs).toInt().coerceAtLeast(0)
            }
            lastKeyframeSentAtMs = afterMs
        }
    }

    fun sendAudio(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        if (!client.isStreaming || awaitingFirstKeyframe || info.size <= 0 || info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) return

        val dup = buffer.duplicate()
        dup.position(info.offset)
        dup.limit(info.offset + info.size)
        val packetInfo = MediaCodec.BufferInfo().apply {
            // Keep AAC's sample-clock spacing (roughly 21.33 ms at 48 kHz) instead of stamping
            // packets with the instant they happen to leave the encoder. Rewriting every buffered
            // AAC packet to "now" collapses timestamps and is heard as crackle/noise or speed-up.
            set(0, info.size, paceAudioAndGetPtsUs(info.presentationTimeUs), info.flags)
        }
        if (previousAudioPacketPtsUs >= 0L && packetInfo.presentationTimeUs <= previousAudioPacketPtsUs) audioPtsMonotonic = false
        previousAudioPacketPtsUs = packetInfo.presentationTimeUs
        val beforeNs = SystemClock.elapsedRealtimeNanos()
        client.sendAudio(dup.slice(), packetInfo)
        lastPublisherEnqueueLatencyMs = ((SystemClock.elapsedRealtimeNanos() - beforeNs) / 1_000_000L).toInt()
        lastAudioSentAtMs = SystemClock.elapsedRealtime()
    }

    private fun paceAudioAndGetPtsUs(sourcePtsUs: Long): Long {
        synchronized(audioPaceLock) {
            ensureRealtimeClock()
            if (firstAudioSourcePtsUs < 0L) firstAudioSourcePtsUs = sourcePtsUs
            val normalizedUs = (sourcePtsUs - firstAudioSourcePtsUs).coerceAtLeast(0L)

            // Do not let an encoder burst upload future audio faster than realtime. Keep the AAC
            // source timeline intact, only waiting when it is genuinely ahead of the live clock.
            var nowNs = SystemClock.elapsedRealtimeNanos()
            var wallUs = ((nowNs - publishClockStartNs).coerceAtLeast(0L) / 1_000L)
            val aheadUs = normalizedUs - wallUs
            if (aheadUs > 2_000L) {
                LockSupport.parkNanos((aheadUs * 1_000L).coerceAtMost(60_000_000L))
                nowNs = SystemClock.elapsedRealtimeNanos()
                wallUs = ((nowNs - publishClockStartNs).coerceAtLeast(0L) / 1_000L)
            }

            val safeUs = minOf(normalizedUs, wallUs + 20_000L)
            synchronized(ptsLock) {
                val next = maxOf(safeUs, lastAudioPtsUs + 1L)
                lastAudioPtsUs = next
                return next
            }
        }
    }

    private fun paceVideoAndGetPtsUs(): Long {
        synchronized(videoPaceLock) {
            ensureRealtimeClock()
            val frameIntervalNs = 1_000_000_000L / targetFps.coerceAtLeast(1)
            val now = SystemClock.elapsedRealtimeNanos()
            if (nextVideoSendNs <= 0L) nextVideoSendNs = now

            val waitNs = nextVideoSendNs - now
            if (waitNs > 0L) {
                // parkNanos avoids millisecond rounding and keeps 30/60 fps pacing stable.
                LockSupport.parkNanos(waitNs.coerceAtMost(100_000_000L))
            }

            val sentAt = SystemClock.elapsedRealtimeNanos()
            nextVideoSendNs = maxOf(nextVideoSendNs + frameIntervalNs, sentAt + frameIntervalNs)
            return realtimePtsUs(video = true, nowNs = sentAt)
        }
    }

    private fun realtimePtsUs(video: Boolean, nowNs: Long = SystemClock.elapsedRealtimeNanos()): Long {
        ensureRealtimeClock()
        val raw = ((nowNs - publishClockStartNs).coerceAtLeast(0L) / 1_000L)
        synchronized(ptsLock) {
            return if (video) {
                val next = maxOf(raw, lastVideoPtsUs + 1L)
                lastVideoPtsUs = next
                next
            } else {
                val next = maxOf(raw, lastAudioPtsUs + 1L)
                lastAudioPtsUs = next
                next
            }
        }
    }

    private fun ensureRealtimeClock() {
        if (publishClockStartNs == 0L) {
            synchronized(ptsLock) {
                if (publishClockStartNs == 0L) {
                    publishClockStartNs = SystemClock.elapsedRealtimeNanos()
                }
            }
        }
    }

    private fun resetRealtimeClock() {
        synchronized(videoPaceLock) {
            synchronized(ptsLock) {
                publishClockStartNs = 0L
                nextVideoSendNs = 0L
                lastVideoPtsUs = -1L
                lastAudioPtsUs = -1L
                firstAudioSourcePtsUs = -1L
            }
        }
    }

    fun metricsSnapshot(): Metrics {
        val now = SystemClock.elapsedRealtime()
        return Metrics(
            lastVideoPacketAgeMs = lastVideoSentAtMs.takeIf { it > 0L }?.let { (now - it).toInt().coerceAtLeast(0) },
            lastAudioPacketAgeMs = lastAudioSentAtMs.takeIf { it > 0L }?.let { (now - it).toInt().coerceAtLeast(0) },
            publisherEnqueueLatencyMs = lastPublisherEnqueueLatencyMs.takeIf { it >= 0 },
            // Pedro's RtmpClient does not expose socket-write latency or internal queue depth here.
            // Leave both unknown instead of reporting a fabricated zero.
            socketWriteLatencyMs = null,
            rtmpQueueDepth = null,
            keyframeIntervalMs = measuredKeyframeIntervalMs.takeIf { it >= 0 },
            videoPtsMonotonic = previousVideoPacketPtsUs.takeIf { it >= 0L }?.let { videoPtsMonotonic },
            audioPtsMonotonic = previousAudioPacketPtsUs.takeIf { it >= 0L }?.let { audioPtsMonotonic },
            reconnectCount = reconnectCount,
        )
    }

    fun stop() {
        stoppedByUser = true
        startRequested = false
        connectionStarted = false
        client.disconnect()
        resetRealtimeClock()
        onState(State(Status.DISCONNECTED, "RTMP publisher stopped"))
    }

    override fun onConnectionStarted(url: String) {
        onState(State(Status.CONNECTING, "RTMP handshake started"))
    }

    override fun onConnectionSuccess() {
        awaitingFirstKeyframe = true
        resetRealtimeClock()
        onState(State(Status.LIVE, "Ingest accepted • waiting for first H.264 keyframe"))
    }

    override fun onConnectionFailed(reason: String) {
        if (!stoppedByUser && client.shouldRetry(reason)) {
            reconnectCount += 1
            awaitingFirstKeyframe = true
            resetRealtimeClock()
            onState(State(Status.RECONNECTING, "Connection lost • retrying ingest"))
            client.reConnect(1500)
        } else {
            connectionStarted = false
            onState(State(Status.ERROR, reason))
        }
    }

    override fun onDisconnect() {
        connectionStarted = false
        awaitingFirstKeyframe = true
        resetRealtimeClock()
        if (!stoppedByUser) onState(State(Status.DISCONNECTED, "RTMP connection closed"))
    }

    override fun onAuthError() {
        onState(State(Status.ERROR, "RTMP authentication failed"))
    }

    override fun onAuthSuccess() = Unit

    override fun onNewBitrate(bitrate: Long) {
        onState(State(Status.LIVE, "Publishing realtime-paced video + audio", bitrate))
    }
}
