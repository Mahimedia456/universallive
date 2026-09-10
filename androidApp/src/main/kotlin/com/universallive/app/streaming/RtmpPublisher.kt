package com.universallive.app.streaming

import android.media.MediaCodec
import android.media.MediaFormat
import com.pedro.common.ConnectChecker
import com.pedro.rtmp.rtmp.RtmpClient
import java.nio.ByteBuffer

/**
 * RTMP publisher that deliberately waits for H.264 codec configuration (SPS/PPS)
 * before opening the ingest connection. This avoids a race where an ingest such as
 * YouTube accepts RTMP but keeps waiting for a decodable video sequence header.
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
    @Volatile private var currentUrl: String = ""
    @Volatile private var stoppedByUser = false
    @Volatile private var startRequested = false
    @Volatile private var videoInfoReady = false
    @Volatile private var connectionStarted = false

    fun start(url: String, width: Int, height: Int, fps: Int, audioEnabled: Boolean) {
        currentUrl = url
        stoppedByUser = false
        startRequested = true
        connectionStarted = false
        client.setOnlyVideo(!audioEnabled)
        if (audioEnabled) client.setAudioInfo(48_000, true)
        client.setVideoResolution(width, height)
        client.setFps(fps)
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
        val dup = buffer.duplicate()
        dup.position(info.offset)
        dup.limit(info.offset + info.size)
        val packetInfo = MediaCodec.BufferInfo().apply {
            set(0, info.size, info.presentationTimeUs, info.flags)
        }
        client.sendVideo(dup.slice(), packetInfo)
    }

    fun sendAudio(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        if (!client.isStreaming || info.size <= 0 || info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) return
        val dup = buffer.duplicate()
        dup.position(info.offset)
        dup.limit(info.offset + info.size)
        val packetInfo = MediaCodec.BufferInfo().apply {
            set(0, info.size, info.presentationTimeUs, info.flags)
        }
        client.sendAudio(dup.slice(), packetInfo)
    }

    fun stop() {
        stoppedByUser = true
        startRequested = false
        connectionStarted = false
        client.disconnect()
        onState(State(Status.DISCONNECTED, "RTMP publisher stopped"))
    }

    override fun onConnectionStarted(url: String) {
        onState(State(Status.CONNECTING, "RTMP handshake started"))
    }

    override fun onConnectionSuccess() {
        onState(State(Status.LIVE, "Ingest accepted • sending H.264/AAC media"))
    }

    override fun onConnectionFailed(reason: String) {
        if (!stoppedByUser && client.shouldRetry(reason)) {
            onState(State(Status.RECONNECTING, "Connection lost • retrying ingest"))
            client.reConnect(1500)
        } else {
            connectionStarted = false
            onState(State(Status.ERROR, reason))
        }
    }

    override fun onDisconnect() {
        connectionStarted = false
        if (!stoppedByUser) onState(State(Status.DISCONNECTED, "RTMP connection closed"))
    }

    override fun onAuthError() {
        onState(State(Status.ERROR, "RTMP authentication failed"))
    }

    override fun onAuthSuccess() = Unit

    override fun onNewBitrate(bitrate: Long) {
        onState(State(Status.LIVE, "Publishing video + audio", bitrate))
    }
}
