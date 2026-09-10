package com.universallive.app.streaming

import android.app.Activity
import android.app.Notification
import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Surface
import android.view.WindowManager
import com.universallive.app.streaming.capture.CaptureSnapshot
import com.universallive.app.streaming.capture.CaptureStatus
import com.universallive.app.streaming.capture.PublishStatus
import com.universallive.app.MainActivity
import kotlin.concurrent.thread
import kotlin.math.sqrt

class ScreenCaptureService : Service() {
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var videoEncoder: MediaCodec? = null
    private var encoderInputSurface: Surface? = null
    private var encoderThread: Thread? = null
    private var streamCompositor: StreamCompositor? = null
    private var facecamCamera: FacecamCameraController? = null
    private var rtmpPublisher: RtmpPublisher? = null
    @Volatile private var rtmpServerUrl = ""
    @Volatile private var streamKey = ""
    @Volatile private var publishTarget = ""
    @Volatile private var publishStatus = PublishStatus.IDLE
    @Volatile private var publishMessage = "RTMP publisher is idle"
    @Volatile private var publishedVideoFrames = 0L
    @Volatile private var publishedVideoBytes = 0L
    @Volatile private var networkBitrateBps = 0L

    private var microphoneRecord: AudioRecord? = null
    private var playbackRecord: AudioRecord? = null
    private var microphoneThread: Thread? = null
    private var playbackThread: Thread? = null
    private var pcmMixer: PcmAudioMixer? = null
    private var aacEncoder: AacAudioEncoder? = null

    @Volatile private var audioEncoderName = ""
    @Volatile private var encodedAudioFrames = 0L
    @Volatile private var encodedAudioBytes = 0L
    @Volatile private var publishedAudioFrames = 0L
    @Volatile private var publishedAudioBytes = 0L

    @Volatile private var encoderRunning = false
    @Volatile private var audioRunning = false
    @Volatile private var framesObserved = 0L
    @Volatile private var encodedVideoFrames = 0L
    @Volatile private var encodedVideoBytes = 0L
    @Volatile private var encoderName = ""
    @Volatile private var encoderWidth = 0
    @Volatile private var encoderHeight = 0
    @Volatile private var encoderFps = 0
    @Volatile private var encoderBitrateKbps = 0

    @Volatile private var microphoneBytes = 0L
    @Volatile private var internalAudioBytes = 0L
    @Volatile private var microphoneLevel = 0f
    @Volatile private var internalAudioLevel = 0f
    @Volatile private var microphoneActive = false
    @Volatile private var internalAudioActive = false
    @Volatile private var microphoneRequested = false
    @Volatile private var internalAudioRequested = false
    @Volatile private var facecamEnabled = false
    @Volatile private var facecamActive = false
    @Volatile private var compositorActive = false
    @Volatile private var facecamLens = "Front"
    @Volatile private var facecamShape = "Circle"
    @Volatile private var facecamX = 0.72f
    @Volatile private var facecamY = 0.08f
    @Volatile private var facecamSize = 0.24f
    @Volatile private var facecamMirrored = true
    @Volatile private var sceneName = "Main"
    @Volatile private var overlayPayload = ""
    @Volatile private var captureMode = "Entire device"
    @Volatile private var captureStartedAtEpochMs = 0L

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            releaseCapture(false)
            publishStatus = PublishStatus.IDLE
            publishMessage = "RTMP publisher is idle"
            networkBitrateBps = 0L
            publishSnapshot(
                status = CaptureStatus.IDLE,
                message = "Android ended the screen/audio capture session",
                audioMessage = "Capture session ended",
            )
            stopSelf()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                publishSnapshot(CaptureStatus.STOPPING, "Stopping H.264 screen/audio pipeline", "Stopping audio capture")
                releaseCapture(true)
                publishStatus = PublishStatus.IDLE
                publishMessage = "RTMP publisher is idle"
                networkBitrateBps = 0L
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                publishSnapshot(CaptureStatus.IDLE, "Screen/audio encoder pipeline stopped", "Audio capture stopped")
                return START_NOT_STICKY
            }
            ACTION_UPDATE_SCENE -> {
                sceneName = intent.getStringExtra(EXTRA_SCENE_NAME) ?: sceneName
                overlayPayload = intent.getStringExtra(EXTRA_OVERLAY_PAYLOAD).orEmpty()
                streamCompositor?.updateOverlays(parseOverlayPayload(overlayPayload))
                publishSnapshot(CaptureStatus.CAPTURING, "Scene updated: $sceneName", currentAudioMessage())
            }
            ACTION_UPDATE_BITRATE -> {
                updateVideoBitrate(intent.getIntExtra(EXTRA_VIDEO_BITRATE_KBPS, encoderBitrateKbps).coerceIn(500, 50000))
            }
            ACTION_START -> startCapture(intent)
        }
        return START_NOT_STICKY
    }

    private fun startCapture(intent: Intent) {
        microphoneRequested = intent.getBooleanExtra(EXTRA_CAPTURE_MIC, true)
        internalAudioRequested = intent.getBooleanExtra(EXTRA_CAPTURE_INTERNAL_AUDIO, true)

        val requestedWidth = intent.getIntExtra(EXTRA_VIDEO_WIDTH, 1920).coerceAtLeast(320)
        val requestedHeight = intent.getIntExtra(EXTRA_VIDEO_HEIGHT, 1080).coerceAtLeast(240)
        encoderFps = intent.getIntExtra(EXTRA_VIDEO_FPS, 60).coerceIn(15, 60)
        encoderBitrateKbps = intent.getIntExtra(EXTRA_VIDEO_BITRATE_KBPS, 8000).coerceIn(500, 50000)
        val orientation = intent.getStringExtra(EXTRA_VIDEO_ORIENTATION) ?: "Landscape"
        rtmpServerUrl = intent.getStringExtra(EXTRA_RTMP_SERVER_URL).orEmpty().trim()
        streamKey = intent.getStringExtra(EXTRA_STREAM_KEY).orEmpty().trim()
        publishTarget = intent.getStringExtra(EXTRA_TARGET_NAME).orEmpty().trim()
        facecamEnabled = intent.getBooleanExtra(EXTRA_FACECAM_ENABLED, false)
        facecamLens = intent.getStringExtra(EXTRA_FACECAM_LENS) ?: "Front"
        facecamShape = intent.getStringExtra(EXTRA_FACECAM_SHAPE) ?: "Circle"
        facecamX = intent.getFloatExtra(EXTRA_FACECAM_X, 0.72f)
        facecamY = intent.getFloatExtra(EXTRA_FACECAM_Y, 0.08f)
        facecamSize = intent.getFloatExtra(EXTRA_FACECAM_SIZE, 0.24f)
        facecamMirrored = intent.getBooleanExtra(EXTRA_FACECAM_MIRRORED, true)
        sceneName = intent.getStringExtra(EXTRA_SCENE_NAME) ?: "Main"
        overlayPayload = intent.getStringExtra(EXTRA_OVERLAY_PAYLOAD).orEmpty()
        captureMode = intent.getStringExtra(EXTRA_CAPTURE_MODE) ?: "Entire device"
        val size = resolveEncoderSize(requestedWidth, requestedHeight, orientation)
        encoderWidth = makeEven(size.first)
        encoderHeight = makeEven(size.second)

        startForegroundCompat(microphoneRequested)
        publishSnapshot(CaptureStatus.STARTING, "Preparing Android H.264 hardware encoder", "Preparing audio capture")

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
        val resultData = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RESULT_DATA)
        }

        if (resultCode != Activity.RESULT_OK || resultData == null) {
            publishError("Missing or invalid screen-capture permission data")
            return
        }

        try {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = manager.getMediaProjection(resultCode, resultData)
            mediaProjection?.registerCallback(projectionCallback, null)

            startRtmpPublisherIfConfigured()
            prepareVideoEncoder()
            val encoderSurface = requireNotNull(encoderInputSurface) { "Encoder input surface was not created" }
            streamCompositor = StreamCompositor(
                encoderSurface = encoderSurface,
                width = encoderWidth,
                height = encoderHeight,
                facecam = FacecamRenderConfig(
                    enabled = facecamEnabled,
                    shape = facecamShape,
                    x = facecamX,
                    y = facecamY,
                    size = facecamSize,
                    mirrored = facecamMirrored,
                ),
                overlays = parseOverlayPayload(overlayPayload),
            ).also { it.start(); compositorActive = true }
            val surface = requireNotNull(streamCompositor).screenInputSurface()
            if (facecamEnabled) {
                facecamCamera = FacecamCameraController(this).also { camera ->
                    camera.start(
                        facecamLens,
                        requireNotNull(streamCompositor).cameraInputSurface(),
                        onStarted = {
                            facecamActive = true
                            publishSnapshot(CaptureStatus.CAPTURING, "Screen + facecam compositor active", currentAudioMessage())
                        },
                        onError = { warning ->
                            facecamActive = false
                            publishMessage = warning
                            publishSnapshot(CaptureStatus.CAPTURING, "Screen compositor active; facecam unavailable", currentAudioMessage())
                        },
                    )
                }
            }
            val densityDpi = resources.displayMetrics.densityDpi.coerceAtLeast(DisplayMetrics.DENSITY_LOW)

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "UniversalLiveH264Capture",
                encoderWidth,
                encoderHeight,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface,
                null,
                null,
            )

            if (virtualDisplay == null) {
                publishError("Android could not create the encoder capture display")
                return
            }

            captureStartedAtEpochMs = System.currentTimeMillis()
            startAudioCapture()
            publishSnapshot(
                CaptureStatus.CAPTURING,
                if (facecamEnabled) "OpenGL compositor is feeding screen + facecam to H.264" else "OpenGL compositor is feeding screen to H.264",
                currentAudioMessage(),
            )
        } catch (t: Throwable) {
            publishError("H.264 capture failed: ${t.message ?: t::class.java.simpleName}")
        }
    }


    private fun parseOverlayPayload(payload: String): List<OverlayRenderConfig> {
        if (payload.isBlank()) return emptyList()
        return payload.split("§").mapNotNull { row ->
            val p = row.split("¦")
            if (p.size < 7) return@mapNotNull null
            runCatching {
                OverlayRenderConfig(
                    kind = p[0], x = p[1].toFloat(), y = p[2].toFloat(), width = p[3].toFloat(),
                    opacity = p[4].toFloat(), text = p[5], assetPath = p[6],
                )
            }.getOrNull()
        }
    }

    private fun updateVideoBitrate(kbps: Int) {
        encoderBitrateKbps = kbps
        try {
            val params = android.os.Bundle().apply {
                putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, kbps * 1000)
            }
            videoEncoder?.setParameters(params)
            publishSnapshot(CaptureStatus.CAPTURING, "Adaptive bitrate updated to ${kbps} Kbps", currentAudioMessage())
        } catch (t: Throwable) {
            publishMessage = "Bitrate update unsupported: ${t.message ?: t::class.java.simpleName}"
            publishSnapshot(CaptureStatus.CAPTURING, "Streaming continues at current encoder bitrate", currentAudioMessage())
        }
    }

    private fun prepareVideoEncoder() {
        val format = MediaFormat.createVideoFormat(VIDEO_MIME, encoderWidth, encoderHeight).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, encoderBitrateKbps * 1000)
            setInteger(MediaFormat.KEY_FRAME_RATE, encoderFps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
            setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)
            if (Build.VERSION.SDK_INT >= 23) {
                setInteger(MediaFormat.KEY_PRIORITY, 0)
            }
        }

        val codec = MediaCodec.createEncoderByType(VIDEO_MIME)
        videoEncoder = codec
        encoderName = codec.name
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoderInputSurface = codec.createInputSurface()
        codec.start()
        encoderRunning = true
        encoderThread = thread(start = true, isDaemon = true, name = "UniversalLiveH264Drain") {
            drainVideoEncoder(codec)
        }
    }

    private fun drainVideoEncoder(codec: MediaCodec) {
        val info = MediaCodec.BufferInfo()
        var nextPublishAt = System.currentTimeMillis() + 750
        try {
            while (encoderRunning && !Thread.currentThread().isInterrupted) {
                val index = codec.dequeueOutputBuffer(info, 10_000)
                when {
                    index >= 0 -> {
                        if (info.size > 0) {
                            encodedVideoBytes += info.size.toLong()
                            val output = codec.getOutputBuffer(index)
                            if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                                encodedVideoFrames++
                                framesObserved = encodedVideoFrames
                                if (output != null && publishStatus == PublishStatus.LIVE) {
                                    rtmpPublisher?.sendVideo(output, info)
                                    publishedVideoFrames++
                                    publishedVideoBytes += info.size.toLong()
                                }
                            }
                        }
                        codec.releaseOutputBuffer(index, false)
                    }
                    index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        rtmpPublisher?.setVideoFormat(codec.outputFormat)
                    }
                }

                val now = System.currentTimeMillis()
                if (now >= nextPublishAt) {
                    publishSnapshot(
                        CaptureStatus.CAPTURING,
                        "H.264 encoding is active",
                        currentAudioMessage(),
                    )
                    nextPublishAt = now + 750
                }
            }
        } catch (_: Throwable) {
            if (encoderRunning) {
                publishSnapshot(CaptureStatus.ERROR, "H.264 encoder stopped unexpectedly", currentAudioMessage())
            }
        }
    }

    private fun startRtmpPublisherIfConfigured() {
        if (rtmpServerUrl.isBlank() || streamKey.isBlank()) {
            publishStatus = PublishStatus.IDLE
            publishMessage = "No active RTMP destination; encoding locally"
            return
        }
        val endpoint = buildPublishUrl(rtmpServerUrl, streamKey)
        publishStatus = PublishStatus.CONNECTING
        publishMessage = "Connecting to ${publishTarget.ifBlank { "RTMP destination" }}"
        rtmpPublisher = RtmpPublisher { state ->
            val wasLive = publishStatus == PublishStatus.LIVE
            publishStatus = when (state.status) {
                RtmpPublisher.Status.IDLE -> PublishStatus.IDLE
                RtmpPublisher.Status.CONNECTING -> PublishStatus.CONNECTING
                RtmpPublisher.Status.LIVE -> PublishStatus.LIVE
                RtmpPublisher.Status.RECONNECTING -> PublishStatus.RECONNECTING
                RtmpPublisher.Status.ERROR -> PublishStatus.ERROR
                RtmpPublisher.Status.DISCONNECTED -> PublishStatus.DISCONNECTED
            }
            publishMessage = state.message
            if (state.bitrateBps > 0) networkBitrateBps = state.bitrateBps
            if (!wasLive && publishStatus == PublishStatus.LIVE) requestSyncFrame()
            publishSnapshot(CaptureStatus.CAPTURING, if (publishStatus == PublishStatus.LIVE) "Platform ingest is receiving the stream" else "H.264 encoding is active", currentAudioMessage())
        }
        rtmpPublisher?.start(endpoint, encoderWidth, encoderHeight, encoderFps, microphoneRequested || internalAudioRequested)
    }

    private fun buildPublishUrl(serverUrl: String, key: String): String {
        return serverUrl.trimEnd('/') + "/" + key.trimStart('/')
    }

    private fun requestSyncFrame() {
        try {
            val params = android.os.Bundle().apply { putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0) }
            videoEncoder?.setParameters(params)
        } catch (_: Throwable) {
            // Encoder will naturally emit the next keyframe.
        }
    }

    private fun stopRtmpPublisher() {
        try { rtmpPublisher?.stop() } catch (_: Throwable) {}
        rtmpPublisher = null
        if (publishStatus != PublishStatus.ERROR) publishStatus = PublishStatus.DISCONNECTED
    }

    private fun resolveEncoderSize(width: Int, height: Int, orientation: String): Pair<Int, Int> {
        return when (orientation.lowercase()) {
            "portrait" -> minOf(width, height) to maxOf(width, height)
            "auto" -> {
                val bounds = if (Build.VERSION.SDK_INT >= 30) {
                    (getSystemService(Context.WINDOW_SERVICE) as WindowManager).maximumWindowMetrics.bounds
                } else null
                val portrait = bounds?.let { it.height() > it.width() } ?: false
                if (portrait) minOf(width, height) to maxOf(width, height)
                else maxOf(width, height) to minOf(width, height)
            }
            else -> maxOf(width, height) to minOf(width, height)
        }
    }

    private fun makeEven(value: Int): Int = if (value % 2 == 0) value else value - 1

    private fun startAudioCapture() {
        if (!microphoneRequested && !internalAudioRequested) {
            publishSnapshot(CaptureStatus.CAPTURING, "H.264 encoding is active", "Audio capture disabled")
            return
        }

        audioRunning = true
        prepareMixedAacAudio()

        if (microphoneRequested) {
            try {
                microphoneRecord = buildMicrophoneRecord().also { record ->
                    record.startRecording()
                    microphoneActive = record.recordingState == AudioRecord.RECORDSTATE_RECORDING
                }
                microphoneThread = startAudioReader(
                    name = "UniversalLiveMic",
                    recordProvider = { microphoneRecord },
                    onRead = { samples, count, bytes, level ->
                        microphoneBytes += bytes
                        microphoneLevel = level
                        pcmMixer?.submitMicrophone(samples, count)
                    },
                )
            } catch (_: Throwable) {
                microphoneActive = false
            }
        }

        if (internalAudioRequested) {
            if (Build.VERSION.SDK_INT < 29) {
                internalAudioActive = false
            } else {
                try {
                    playbackRecord = buildPlaybackRecord().also { record ->
                        record.startRecording()
                        internalAudioActive = record.recordingState == AudioRecord.RECORDSTATE_RECORDING
                    }
                    playbackThread = startAudioReader(
                        name = "UniversalLivePlayback",
                        recordProvider = { playbackRecord },
                        onRead = { samples, count, bytes, level ->
                            internalAudioBytes += bytes
                            internalAudioLevel = level
                            pcmMixer?.submitPlaybackStereo(samples, count)
                        },
                    )
                } catch (_: Throwable) {
                    internalAudioActive = false
                }
            }
        }
    }

    private fun prepareMixedAacAudio() {
        if (!microphoneRequested && !internalAudioRequested) return

        val encoder = AacAudioEncoder(
            sampleRate = SAMPLE_RATE,
            channels = 2,
            bitrate = AUDIO_BITRATE_BPS,
            onEncoded = { buffer, info ->
                encodedAudioFrames++
                encodedAudioBytes += info.size.toLong()
                if (publishStatus == PublishStatus.LIVE) {
                    rtmpPublisher?.sendAudio(buffer, info)
                    publishedAudioFrames++
                    publishedAudioBytes += info.size.toLong()
                }
            },
            onFormat = { /* AAC-LC stream metadata is configured through RTMP sample rate/stereo info. */ },
            onError = { error ->
                publishMessage = "AAC audio warning: $error"
            },
        )
        try {
            encoder.start()
            audioEncoderName = encoder.encoderName
            aacEncoder = encoder
            pcmMixer = PcmAudioMixer(
                sampleRate = SAMPLE_RATE,
                microphoneEnabled = microphoneRequested,
                playbackEnabled = internalAudioRequested,
                onMixedFrame = { mixed -> aacEncoder?.queuePcmStereo(mixed) },
            ).also { it.start() }
        } catch (t: Throwable) {
            audioEncoderName = ""
            publishMessage = "AAC encoder unavailable: ${t.message ?: t::class.java.simpleName}"
            try { encoder.stop() } catch (_: Throwable) {}
        }
    }

    private fun buildMicrophoneRecord(): AudioRecord {
        val min = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(SAMPLE_RATE / 5)
        return AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(min * 2)
            .build()
    }

    private fun buildPlaybackRecord(): AudioRecord {
        check(Build.VERSION.SDK_INT >= 29)
        val projection = requireNotNull(mediaProjection)
        val config = AudioPlaybackCaptureConfiguration.Builder(projection)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()
        val min = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(SAMPLE_RATE / 2)
        return AudioRecord.Builder()
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                    .build(),
            )
            .setBufferSizeInBytes(min * 2)
            .setAudioPlaybackCaptureConfig(config)
            .build()
    }

    private fun startAudioReader(
        name: String,
        recordProvider: () -> AudioRecord?,
        onRead: (ShortArray, Int, Long, Float) -> Unit,
    ): Thread = thread(start = true, isDaemon = true, name = name) {
        val buffer = ShortArray(4096)
        while (audioRunning && !Thread.currentThread().isInterrupted) {
            val record = recordProvider() ?: break
            val read = try {
                record.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
            } catch (_: Throwable) {
                break
            }
            if (read > 0) {
                onRead(buffer.copyOf(read), read, read.toLong() * 2L, rmsLevel(buffer, read))
            }
        }
    }

    private fun rmsLevel(samples: ShortArray, count: Int): Float {
        if (count <= 0) return 0f
        var sum = 0.0
        for (i in 0 until count) {
            val normalized = samples[i].toDouble() / Short.MAX_VALUE.toDouble()
            sum += normalized * normalized
        }
        return sqrt(sum / count).toFloat().coerceIn(0f, 1f)
    }

    private fun currentAudioMessage(): String {
        if (!microphoneRequested && !internalAudioRequested) return "Audio capture disabled"
        val parts = mutableListOf<String>()
        if (microphoneRequested) parts += if (microphoneActive) "microphone active" else "microphone unavailable"
        if (internalAudioRequested) {
            parts += when {
                Build.VERSION.SDK_INT < 29 -> "game audio requires Android 10+"
                internalAudioActive -> "game audio active"
                else -> "game audio unavailable or blocked"
            }
        }
        if (audioEncoderName.isNotBlank()) parts += "AAC live"
        return parts.joinToString(" • ")
    }

    private fun startForegroundCompat(includeMicrophone: Boolean) {
        val notification = buildLiveNotification("STARTING", "Preparing screen capture")
        if (Build.VERSION.SDK_INT >= 29) {
            var serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            if (includeMicrophone && Build.VERSION.SDK_INT >= 30) {
                serviceType = serviceType or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            }
            startForeground(NOTIFICATION_ID, notification, serviceType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildLiveNotification(status: String, detail: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 101, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this, 102, Intent(this, ScreenCaptureService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("UniversalLive • $status")
            .setContentText(detail)
            .setSubText(publishTarget.ifBlank { captureMode })
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_media_pause, "STOP LIVE", stopIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun updateLiveNotification(snapshot: CaptureSnapshot) {
        if (!snapshot.isActive && snapshot.status != CaptureStatus.ERROR) return
        val status = when (snapshot.publishStatus) {
            PublishStatus.LIVE -> "LIVE"
            PublishStatus.CONNECTING -> "CONNECTING"
            PublishStatus.RECONNECTING -> "RECONNECTING"
            PublishStatus.ERROR -> "STREAM ERROR"
            else -> if (snapshot.status == CaptureStatus.CAPTURING) "CAPTURING" else snapshot.status.name
        }
        val kbps = snapshot.networkBitrateBps / 1000L
        val detail = when {
            snapshot.publishStatus == PublishStatus.LIVE && kbps > 0 -> "${kbps} Kbps • ${snapshot.publishedVideoFrames} video • ${snapshot.publishedAudioFrames} audio"
            snapshot.publishStatus == PublishStatus.LIVE -> "Ingest accepted • sending media"
            else -> snapshot.publishMessage.takeIf { it.isNotBlank() } ?: snapshot.message
        }
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildLiveNotification(status, detail))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, "Live capture and encoding", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun publishSnapshot(status: CaptureStatus, message: String, audioMessage: String) {
        val snapshot = CaptureSnapshot(
            status = status,
            message = message,
            framesObserved = framesObserved,
            encodedVideoFrames = encodedVideoFrames,
            encodedVideoBytes = encodedVideoBytes,
            encoderName = encoderName,
            encoderWidth = encoderWidth,
            encoderHeight = encoderHeight,
            encoderFps = encoderFps,
            encoderBitrateKbps = encoderBitrateKbps,
            microphoneRequested = microphoneRequested,
            internalAudioRequested = internalAudioRequested,
            microphoneActive = microphoneActive,
            internalAudioActive = internalAudioActive,
            microphoneBytes = microphoneBytes,
            internalAudioBytes = internalAudioBytes,
            microphoneLevel = microphoneLevel,
            internalAudioLevel = internalAudioLevel,
            audioEncoderName = audioEncoderName,
            encodedAudioFrames = encodedAudioFrames,
            encodedAudioBytes = encodedAudioBytes,
            publishedAudioFrames = publishedAudioFrames,
            publishedAudioBytes = publishedAudioBytes,
            audioMessage = audioMessage,
            publishStatus = publishStatus,
            publishMessage = publishMessage,
            publishTarget = publishTarget,
            publishedVideoFrames = publishedVideoFrames,
            publishedVideoBytes = publishedVideoBytes,
            networkBitrateBps = networkBitrateBps,
            facecamActive = facecamActive,
            compositorActive = compositorActive,
            activeSceneName = sceneName,
            captureMode = captureMode,
            startedAtEpochMs = captureStartedAtEpochMs,
        )

        CaptureBridge.publish(snapshot)
        updateLiveNotification(snapshot)
    }

    private fun publishError(message: String) {
        publishSnapshot(CaptureStatus.ERROR, message, currentAudioMessage())
        releaseCapture(true)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopAudioCapture() {
        audioRunning = false
        try { pcmMixer?.stop() } catch (_: Throwable) {}
        pcmMixer = null
        try { aacEncoder?.stop() } catch (_: Throwable) {}
        aacEncoder = null
        listOf(microphoneRecord, playbackRecord).forEach { record ->
            try { record?.stop() } catch (_: Throwable) {}
        }
        listOf(microphoneThread, playbackThread).forEach { worker ->
            try { worker?.interrupt() } catch (_: Throwable) {}
        }
        microphoneThread = null
        playbackThread = null
        listOf(microphoneRecord, playbackRecord).forEach { record ->
            try { record?.release() } catch (_: Throwable) {}
        }
        microphoneRecord = null
        playbackRecord = null
        microphoneActive = false
        internalAudioActive = false
        microphoneLevel = 0f
        internalAudioLevel = 0f
    }

    private fun stopVideoEncoder() {
        encoderRunning = false
        try { encoderThread?.interrupt() } catch (_: Throwable) {}
        encoderThread = null
        try { videoEncoder?.signalEndOfInputStream() } catch (_: Throwable) {}
        try { videoEncoder?.stop() } catch (_: Throwable) {}
        try { videoEncoder?.release() } catch (_: Throwable) {}
        videoEncoder = null
        try { encoderInputSurface?.release() } catch (_: Throwable) {}
        encoderInputSurface = null
    }

    private fun releaseCapture(stopProjection: Boolean) {
        stopAudioCapture()
        virtualDisplay?.release()
        virtualDisplay = null
        stopRtmpPublisher()
        try { facecamCamera?.stop() } catch (_: Throwable) {}
        facecamCamera = null
        facecamActive = false
        try { streamCompositor?.release() } catch (_: Throwable) {}
        streamCompositor = null
        compositorActive = false
        stopVideoEncoder()
        if (stopProjection) {
            try { mediaProjection?.unregisterCallback(projectionCallback) } catch (_: Throwable) {}
            try { mediaProjection?.stop() } catch (_: Throwable) {}
        }
        mediaProjection = null
    }

    override fun onDestroy() {
        releaseCapture(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.universallive.app.action.START_CAPTURE"
        const val ACTION_UPDATE_SCENE = "com.universallive.app.action.UPDATE_SCENE"
        const val ACTION_UPDATE_BITRATE = "com.universallive.app.action.UPDATE_BITRATE"
        const val ACTION_STOP = "com.universallive.app.action.STOP_CAPTURE"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val EXTRA_CAPTURE_MIC = "capture_mic"
        const val EXTRA_CAPTURE_INTERNAL_AUDIO = "capture_internal_audio"
        const val EXTRA_VIDEO_WIDTH = "video_width"
        const val EXTRA_VIDEO_HEIGHT = "video_height"
        const val EXTRA_VIDEO_FPS = "video_fps"
        const val EXTRA_VIDEO_BITRATE_KBPS = "video_bitrate_kbps"
        const val EXTRA_VIDEO_ORIENTATION = "video_orientation"
        const val EXTRA_RTMP_SERVER_URL = "rtmp_server_url"
        const val EXTRA_STREAM_KEY = "stream_key"
        const val EXTRA_TARGET_NAME = "target_name"
        const val EXTRA_FACECAM_ENABLED = "facecam_enabled"
        const val EXTRA_FACECAM_LENS = "facecam_lens"
        const val EXTRA_FACECAM_SHAPE = "facecam_shape"
        const val EXTRA_FACECAM_X = "facecam_x"
        const val EXTRA_FACECAM_Y = "facecam_y"
        const val EXTRA_FACECAM_SIZE = "facecam_size"
        const val EXTRA_FACECAM_MIRRORED = "facecam_mirrored"
        const val EXTRA_SCENE_NAME = "scene_name"
        const val EXTRA_OVERLAY_PAYLOAD = "overlay_payload"
        const val EXTRA_CAPTURE_MODE = "capture_mode"
        private const val CHANNEL_ID = "screen_capture"
        private const val NOTIFICATION_ID = 4106
        private const val SAMPLE_RATE = 48_000
        private const val AUDIO_BITRATE_BPS = 160_000
        private const val VIDEO_MIME = "video/avc"
    }
}