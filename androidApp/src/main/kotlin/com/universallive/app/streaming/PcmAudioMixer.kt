package com.universallive.app.streaming

import kotlin.concurrent.thread
import kotlin.math.roundToInt

/**
 * 48 kHz PCM16 mixer. Microphone input is mono; playback input is stereo.
 * It emits fixed 20 ms stereo frames (960 samples/channel) suitable for AAC.
 */
class PcmAudioMixer(
    private val sampleRate: Int = 48_000,
    private val microphoneEnabled: Boolean,
    private val playbackEnabled: Boolean,
    private val microphoneGain: Float = 1.0f,
    private val playbackGain: Float = 1.0f,
    private val onMixedFrame: (ShortArray) -> Unit,
) {
    private val micRing = ShortRingBuffer(sampleRate * 2)
    private val gameRing = ShortRingBuffer(sampleRate * 4)
    @Volatile private var running = false
    private var worker: Thread? = null

    fun start() {
        if (running) return
        running = true
        worker = thread(start = true, isDaemon = true, name = "UniversalLiveAudioMixer") {
            val framesPerChunk = sampleRate / 50 // 20 ms
            val stereoShorts = framesPerChunk * 2
            val frameNanos = 20_000_000L
            var nextTick = System.nanoTime()

            while (running && !Thread.currentThread().isInterrupted) {
                nextTick += frameNanos
                val mic = if (microphoneEnabled) micRing.readOrSilence(framesPerChunk) else ShortArray(framesPerChunk)
                val game = if (playbackEnabled) gameRing.readOrSilence(stereoShorts) else ShortArray(stereoShorts)
                val mixed = ShortArray(stereoShorts)

                for (i in 0 until framesPerChunk) {
                    val micSample = mic[i].toInt()
                    val left = (micSample * microphoneGain + game[i * 2] * playbackGain).roundToInt()
                    val right = (micSample * microphoneGain + game[i * 2 + 1] * playbackGain).roundToInt()
                    mixed[i * 2] = saturate(left)
                    mixed[i * 2 + 1] = saturate(right)
                }
                onMixedFrame(mixed)

                val sleepNanos = nextTick - System.nanoTime()
                if (sleepNanos > 0) {
                    try {
                        val millis = sleepNanos / 1_000_000L
                        val nanos = (sleepNanos % 1_000_000L).toInt()
                        Thread.sleep(millis, nanos)
                    } catch (_: InterruptedException) {
                        break
                    }
                } else if (sleepNanos < -100_000_000L) {
                    nextTick = System.nanoTime()
                }
            }
        }
    }

    fun submitMicrophone(samples: ShortArray, count: Int) {
        if (microphoneEnabled && count > 0) micRing.write(samples, count)
    }

    fun submitPlaybackStereo(samples: ShortArray, count: Int) {
        if (playbackEnabled && count > 0) gameRing.write(samples, count)
    }

    fun stop() {
        running = false
        try { worker?.interrupt() } catch (_: Throwable) {}
        worker = null
        micRing.clear()
        gameRing.clear()
    }

    private fun saturate(value: Int): Short = value.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

    private class ShortRingBuffer(capacity: Int) {
        private val data = ShortArray(capacity.coerceAtLeast(4096))
        private var readIndex = 0
        private var writeIndex = 0
        private var size = 0

        @Synchronized
        fun write(source: ShortArray, count: Int) {
            var remaining = count.coerceAtMost(source.size)
            var src = 0
            while (remaining > 0) {
                if (size == data.size) {
                    readIndex = (readIndex + 1) % data.size
                    size--
                }
                data[writeIndex] = source[src]
                writeIndex = (writeIndex + 1) % data.size
                size++
                src++
                remaining--
            }
        }

        @Synchronized
        fun readOrSilence(count: Int): ShortArray {
            val out = ShortArray(count)
            val available = minOf(count, size)
            for (i in 0 until available) {
                out[i] = data[readIndex]
                readIndex = (readIndex + 1) % data.size
            }
            size -= available
            return out
        }

        @Synchronized
        fun clear() {
            readIndex = 0
            writeIndex = 0
            size = 0
        }
    }
}
