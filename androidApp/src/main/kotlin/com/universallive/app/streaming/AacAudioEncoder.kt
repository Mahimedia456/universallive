package com.universallive.app.streaming

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import java.nio.ByteOrder
import kotlin.concurrent.thread

class AacAudioEncoder(
    private val sampleRate: Int = 48_000,
    private val channels: Int = 2,
    private val bitrate: Int = 160_000,
    private val onEncoded: (java.nio.ByteBuffer, MediaCodec.BufferInfo) -> Unit,
    private val onFormat: (MediaFormat) -> Unit,
    private val onError: (String) -> Unit,
) {
    private var codec: MediaCodec? = null
    private var drainThread: Thread? = null
    @Volatile private var running = false
    private var submittedFrames = 0L

    @Volatile var encoderName: String = ""
        private set
    @Volatile var encodedFrames: Long = 0
        private set
    @Volatile var encodedBytes: Long = 0
        private set

    fun start() {
        if (running) return
        val format = MediaFormat.createAudioFormat(MIME, sampleRate, channels).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16 * 1024)
        }
        val c = MediaCodec.createEncoderByType(MIME)
        codec = c
        encoderName = c.name
        c.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        c.start()
        running = true
        drainThread = thread(start = true, isDaemon = true, name = "UniversalLiveAacDrain") { drain(c) }
    }

    fun queuePcmStereo(samples: ShortArray) {
        if (!running || samples.isEmpty()) return
        val c = codec ?: return
        try {
            val index = c.dequeueInputBuffer(10_000)
            if (index < 0) return
            val input = c.getInputBuffer(index) ?: return
            input.clear()
            input.order(ByteOrder.LITTLE_ENDIAN)
            val shorts = input.asShortBuffer()
            val toWrite = minOf(samples.size, shorts.remaining())
            shorts.put(samples, 0, toWrite)
            val bytes = toWrite * 2
            val stereoFrames = toWrite / channels
            val ptsUs = submittedFrames * 1_000_000L / sampleRate
            submittedFrames += stereoFrames
            c.queueInputBuffer(index, 0, bytes, ptsUs, 0)
        } catch (t: Throwable) {
            if (running) onError(t.message ?: "AAC input failed")
        }
    }

    private fun drain(c: MediaCodec) {
        val info = MediaCodec.BufferInfo()
        try {
            while (running && !Thread.currentThread().isInterrupted) {
                val index = c.dequeueOutputBuffer(info, 10_000)
                when {
                    index >= 0 -> {
                        val output = c.getOutputBuffer(index)
                        if (output != null && info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            encodedFrames++
                            encodedBytes += info.size
                            val dup = output.duplicate()
                            dup.position(info.offset)
                            dup.limit(info.offset + info.size)
                            val packetInfo = MediaCodec.BufferInfo().apply {
                                set(0, info.size, info.presentationTimeUs, info.flags)
                            }
                            onEncoded(dup.slice(), packetInfo)
                        }
                        c.releaseOutputBuffer(index, false)
                    }
                    index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> onFormat(c.outputFormat)
                }
            }
        } catch (t: Throwable) {
            if (running) onError(t.message ?: "AAC encoder stopped")
        }
    }

    fun stop() {
        running = false
        try { drainThread?.interrupt() } catch (_: Throwable) {}
        drainThread = null
        try { codec?.stop() } catch (_: Throwable) {}
        try { codec?.release() } catch (_: Throwable) {}
        codec = null
    }

    companion object {
        private const val MIME = "audio/mp4a-latm"
    }
}
