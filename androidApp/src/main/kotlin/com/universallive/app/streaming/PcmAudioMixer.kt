package com.universallive.app.streaming

import kotlin.concurrent.thread
import kotlin.math.roundToInt

/**
 * 48 kHz PCM16 mixer. Microphone input is mono; playback input is stereo.
 * It emits AAC-LC sized 1024-sample stereo frames with headroom, a light mic noise gate,
 * and soft limiting to avoid the clipping/static heard when game audio and microphone overlap.
 */
class PcmAudioMixer(
    private val sampleRate: Int = 48_000,
    private val microphoneEnabled: Boolean,
    private val playbackEnabled: Boolean,
    private val microphoneGain: Float = 0.70f,
    private val playbackGain: Float = 0.85f,
    private val masterGain: Float = 0.90f,
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
            val framesPerChunk = 1024 // AAC-LC frame size per channel
            val stereoShorts = framesPerChunk * 2
            val frameNanos = framesPerChunk * 1_000_000_000L / sampleRate
            var nextTick = System.nanoTime()

            while (running && !Thread.currentThread().isInterrupted) {
                nextTick += frameNanos
                val mic = if (microphoneEnabled) micRing.readOrSilence(framesPerChunk) else ShortArray(framesPerChunk)
                val game = if (playbackEnabled) gameRing.readOrSilence(stereoShorts) else ShortArray(stereoShorts)
                val mixed = ShortArray(stereoShorts)

                // Gate only very low-level microphone noise; do not gate normal speech.
                var micEnergy = 0.0
                if (microphoneEnabled) {
                    for (sample in mic) {
                        val n = sample.toDouble() / Short.MAX_VALUE.toDouble()
                        micEnergy += n * n
                    }
                }
                val micRms = if (mic.isNotEmpty()) kotlin.math.sqrt(micEnergy / mic.size) else 0.0
                val micGate = if (micRms < 0.0065) 0f else 1f

                for (i in 0 until framesPerChunk) {
                    val micSample = mic[i].toFloat() * microphoneGain * micGate
                    val left = (micSample + game[i * 2].toFloat() * playbackGain) * masterGain
                    val right = (micSample + game[i * 2 + 1].toFloat() * playbackGain) * masterGain
                    mixed[i * 2] = softLimit(left)
                    mixed[i * 2 + 1] = softLimit(right)
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

    private fun softLimit(value: Float): Short {
        val normalized = value / Short.MAX_VALUE.toFloat()
        // Smooth limiter: linear around normal levels and progressively compresses peaks.
        val limited = normalized / (1f + kotlin.math.abs(normalized) * 0.55f)
        return (limited.coerceIn(-1f, 1f) * Short.MAX_VALUE).roundToInt().toShort()
    }

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
