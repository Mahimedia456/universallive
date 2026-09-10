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

    private val client = RtmpClient(this)
    private val videoPaceLock = Any()
    private val ptsLock = Any()

    @Volatile private var currentUrl: String = ""
    @Volatile private var stoppedByUser = false
    @Volatile private var startRequested = false
    @Volatile private var videoInfoReady = false
    @Volatile private var connectionStarted = false
    @Volatile private var targetFps = 30
    @Volatile private var publishClockStartNs = 0L

    private var nextVideoSendNs = 0L
    private var lastVideoPtsUs = -1L
    private var lastAudioPtsUs = -1L

    fun start(url: String, width: Int, height: Int, fps: Int, audioEnabled: Boolean) {
        currentUrl = url
        stoppedByUser = false
        startRequested = true
        connectionStarted = false
        targetFps = fps.coerceIn(15, 60)
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

        // Keep upload cadence tied to wall clock. This prevents a buffered MediaCodec drain from
        // transmitting several seconds of video during one real second.
        val ptsUs = paceVideoAndGetPtsUs()

        val dup = buffer.duplicate()
        dup.position(info.offset)
        dup.limit(info.offset + info.size)
        val packetInfo = MediaCodec.BufferInfo().apply {
            set(0, info.size, ptsUs, info.flags)
        }
        client.sendVideo(dup.slice(), packetInfo)
    }

    fun sendAudio(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        if (!client.isStreaming || info.size <= 0 || info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) return

        val dup = buffer.duplicate()
        dup.position(info.offset)
        dup.limit(info.offset + info.size)
        val packetInfo = MediaCodec.BufferInfo().apply {
            set(0, info.size, realtimePtsUs(video = false), info.flags)
        }
        client.sendAudio(dup.slice(), packetInfo)
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
            }
        }
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
        resetRealtimeClock()
        onState(State(Status.LIVE, "Ingest accepted • realtime-paced H.264/AAC active"))
    }

    override fun onConnectionFailed(reason: String) {
        if (!stoppedByUser && client.shouldRetry(reason)) {
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
