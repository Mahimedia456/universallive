package com.universallive.app.streaming

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.SurfaceTexture
import android.graphics.Typeface
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import com.universallive.app.streaming.preview.AndroidPreviewBridge
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

internal data class FacecamRenderConfig(
    val enabled: Boolean,
    val shape: String,
    val x: Float,
    val y: Float,
    val size: Float,
    val mirrored: Boolean,
)

internal data class OverlayRenderConfig(
    val kind: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val opacity: Float,
    val text: String,
    val assetPath: String,
)

private data class PreparedOverlay(
    val config: OverlayRenderConfig,
    val textureId: Int,
    val aspect: Float,
)

internal class StreamCompositor(
    private val encoderSurface: Surface,
    private val width: Int,
    private val height: Int,
    initialScreenWidth: Int,
    initialScreenHeight: Int,
    private val facecam: FacecamRenderConfig,
    private var overlays: List<OverlayRenderConfig> = emptyList(),
) {
    private val thread = HandlerThread("UniversalLiveGL")
    private lateinit var handler: Handler

    private var egl: EglWindow? = null
    private var renderer: ExternalTextureRenderer? = null
    private var overlayRenderer: BitmapOverlayRenderer? = null

    private var screenTextureId = 0
    private var cameraTextureId = 0

    private var screenTexture: SurfaceTexture? = null
    private var cameraTexture: SurfaceTexture? = null
    private var screenSurface: Surface? = null
    private var cameraSurface: Surface? = null

    private val renderPending = AtomicBoolean(false)
    private val screenMatrix = FloatArray(16)
    private val cameraMatrix = FloatArray(16)

    private val prepared = mutableListOf<PreparedOverlay>()

    @Volatile
    private var screenFrameReady = false

    @Volatile
    private var cameraFrameReady = false

    private var lastPreviewAtMs = 0L
    @Volatile private var screenInputWidth = initialScreenWidth.coerceAtLeast(2)
    @Volatile private var screenInputHeight = initialScreenHeight.coerceAtLeast(2)

    fun start() {
        thread.start()
        handler = Handler(thread.looper)

        val latch = CountDownLatch(1)
        var error: Throwable? = null

        handler.post {
            try {
                egl = EglWindow(encoderSurface).also { it.create() }
                renderer = ExternalTextureRenderer().also { it.create() }
                overlayRenderer = BitmapOverlayRenderer().also { it.create() }

                screenTextureId = renderer!!.createExternalTexture()
                cameraTextureId = renderer!!.createExternalTexture()

                screenTexture = SurfaceTexture(screenTextureId).apply {
                    setDefaultBufferSize(screenInputWidth, screenInputHeight)
                    setOnFrameAvailableListener({
                        screenFrameReady = true
                        requestRender()
                    }, handler)
                }

                cameraTexture = SurfaceTexture(cameraTextureId).apply {
                    setDefaultBufferSize(1280, 720)
                    setOnFrameAvailableListener({
                        cameraFrameReady = true
                        requestRender()
                    }, handler)
                }

                screenSurface = Surface(screenTexture)
                cameraSurface = Surface(cameraTexture)

                prepareOverlays()
            } catch (t: Throwable) {
                error = t
            } finally {
                latch.countDown()
            }
        }

        check(latch.await(5, TimeUnit.SECONDS)) {
            "Timed out starting GL compositor"
        }

        error?.let { throw it }
    }

    fun screenInputSurface(): Surface = requireNotNull(screenSurface)

    fun cameraInputSurface(): Surface = requireNotNull(cameraSurface)

    fun updateScreenInputSize(nextWidth: Int, nextHeight: Int) {
        screenInputWidth = nextWidth.coerceAtLeast(2)
        screenInputHeight = nextHeight.coerceAtLeast(2)
        if (!::handler.isInitialized) return
        handler.post {
            screenTexture?.setDefaultBufferSize(screenInputWidth, screenInputHeight)
            if (screenFrameReady) requestRender()
        }
    }

    fun updateOverlays(next: List<OverlayRenderConfig>) {
        if (!::handler.isInitialized) return

        handler.post {
            prepared.forEach { overlayRenderer?.delete(it.textureId) }
            prepared.clear()
            overlays = next
            prepareOverlays()

            if (screenFrameReady) {
                requestRender()
            }
        }
    }

    private fun requestRender() {
        if (!::handler.isInitialized) return
        if (!renderPending.compareAndSet(false, true)) return

        handler.post {
            try {
                renderFrame()
            } finally {
                renderPending.set(false)
            }
        }
    }

    private fun prepareOverlays() {
        val bitmapRenderer = overlayRenderer ?: return

        overlays.forEach { config ->
            createBitmap(config)?.let { bitmap ->
                val textureId = bitmapRenderer.upload(bitmap)
                prepared += PreparedOverlay(
                    config = config,
                    textureId = textureId,
                    aspect = bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1),
                )
                bitmap.recycle()
            }
        }
    }

    private fun createBitmap(config: OverlayRenderConfig): Bitmap? =
        when (config.kind.uppercase()) {
            "IMAGE" -> config.assetPath
                .takeIf { it.isNotBlank() && File(it).exists() }
                ?.let { BitmapFactory.decodeFile(it) }

            else -> {
                val label =
                    (if (config.kind.equals("LOGO", true) && config.text.isBlank()) {
                        "UNIVERSAL LIVE"
                    } else {
                        config.text
                    }).ifBlank { "LIVE" }

                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 72f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                val pad = 28
                val textWidth = paint.measureText(label).toInt().coerceAtLeast(1)
                val fontMetrics = paint.fontMetrics
                val textHeight =
                    (fontMetrics.descent - fontMetrics.ascent).toInt().coerceAtLeast(1)

                Bitmap.createBitmap(
                    textWidth + pad * 2,
                    textHeight + pad * 2,
                    Bitmap.Config.ARGB_8888,
                ).apply {
                    Canvas(this).drawText(
                        label,
                        pad.toFloat(),
                        pad - fontMetrics.ascent,
                        paint,
                    )
                }
            }
        }

    private fun renderFrame() {
        if (!screenFrameReady) {
            return
        }

        val eglWindow = egl ?: return
        val externalRenderer = renderer ?: return

        eglWindow.makeCurrent()

        screenTexture?.updateTexImage()
        screenTexture?.getTransformMatrix(screenMatrix)

        if (cameraFrameReady) {
            cameraTexture?.updateTexImage()
            cameraTexture?.getTransformMatrix(cameraMatrix)
        }

        GLES20.glViewport(0, 0, width, height)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // Fill the encoder canvas without stretching. Ultra-wide gaming phones are wider than
        // 16:9, so crop only the excess edge area. When the phone rotates, the MediaProjection
        // input dimensions are updated first and the crop is recalculated on the next frame.
        val sourceAspect = screenInputWidth.toFloat() / screenInputHeight.coerceAtLeast(1)
        val targetAspect = width.toFloat() / height.coerceAtLeast(1)
        val cropScaleX: Float
        val cropScaleY: Float
        if (sourceAspect > targetAspect) {
            cropScaleX = (targetAspect / sourceAspect).coerceIn(0.05f, 1f)
            cropScaleY = 1f
        } else {
            cropScaleX = 1f
            cropScaleY = (sourceAspect / targetAspect).coerceIn(0.05f, 1f)
        }
        GLES20.glViewport(0, 0, width, height)

        externalRenderer.draw(
            texture = screenTextureId,
            matrix = screenMatrix,
            mirrored = false,
            mask = OverlayMask.NONE,
            cropScaleX = cropScaleX,
            cropScaleY = cropScaleY,
        )

        if (facecam.enabled && cameraFrameReady) {
            val sizePx =
                (facecam.size.coerceIn(0.12f, 0.5f) * width)
                    .toInt()
                    .coerceAtLeast(96)

            val xPx =
                (facecam.x.coerceIn(0f, 1f) * width)
                    .toInt()
                    .coerceIn(0, (width - sizePx).coerceAtLeast(0))

            val topPx =
                (facecam.y.coerceIn(0f, 1f) * height)
                    .toInt()

            val yPx =
                (height - topPx - sizePx)
                    .coerceIn(0, (height - sizePx).coerceAtLeast(0))

            GLES20.glViewport(xPx, yPx, sizePx, sizePx)

            val mask = when (facecam.shape.lowercase()) {
                "circle" -> OverlayMask.CIRCLE
                "rounded" -> OverlayMask.ROUNDED
                else -> OverlayMask.NONE
            }

            externalRenderer.draw(
                texture = cameraTextureId,
                matrix = cameraMatrix,
                mirrored = facecam.mirrored,
                mask = mask,
            )
        }

        overlayRenderer?.let { bitmapRenderer ->
            prepared.forEach { preparedOverlay ->
                val overlayWidth =
                    (preparedOverlay.config.width.coerceIn(0.05f, 1f) * width)
                        .toInt()
                        .coerceAtLeast(24)

                val overlayHeight =
                    (overlayWidth / preparedOverlay.aspect.coerceAtLeast(0.1f))
                        .toInt()
                        .coerceAtLeast(20)

                val overlayX =
                    (preparedOverlay.config.x.coerceIn(0f, 1f) * width)
                        .toInt()
                        .coerceIn(0, (width - overlayWidth).coerceAtLeast(0))

                val top =
                    (preparedOverlay.config.y.coerceIn(0f, 1f) * height)
                        .toInt()

                val overlayY =
                    (height - top - overlayHeight)
                        .coerceIn(0, (height - overlayHeight).coerceAtLeast(0))

                GLES20.glViewport(
                    overlayX,
                    overlayY,
                    overlayWidth,
                    overlayHeight,
                )

                bitmapRenderer.draw(
                    preparedOverlay.textureId,
                    preparedOverlay.config.opacity,
                )
            }
        }

        maybePublishPreview()

        // MediaCodec Surface PTS must advance in realtime. Some vendor SurfaceTexture timestamps
        // jump during rotation/app switches, so use the monotonic clock for the encoder surface.
        eglWindow.swap(System.nanoTime())
    }

    private fun maybePublishPreview() {
        if (!AndroidPreviewBridge.requested) return

        val now = System.currentTimeMillis()
        if (now - lastPreviewAtMs < 900L) return
        lastPreviewAtMs = now

        try {
            GLES20.glViewport(0, 0, width, height)

            val buffer =
                ByteBuffer
                    .allocateDirect(width * height * 4)
                    .order(ByteOrder.nativeOrder())

            GLES20.glReadPixels(
                0,
                0,
                width,
                height,
                GLES20.GL_RGBA,
                GLES20.GL_UNSIGNED_BYTE,
                buffer,
            )

            buffer.rewind()

            val raw =
                Bitmap.createBitmap(
                    width,
                    height,
                    Bitmap.Config.ARGB_8888,
                )

            raw.copyPixelsFromBuffer(buffer)

            val flipMatrix = Matrix().apply {
                postScale(1f, -1f)
            }

            val flipped =
                Bitmap.createBitmap(
                    raw,
                    0,
                    0,
                    width,
                    height,
                    flipMatrix,
                    true,
                )

            if (flipped !== raw) {
                raw.recycle()
            }

            val previewWidth = 480.coerceAtMost(width)
            val previewHeight =
                ((height.toFloat() / width.coerceAtLeast(1)) * previewWidth)
                    .toInt()
                    .coerceAtLeast(1)

            val scaled =
                if (flipped.width != previewWidth) {
                    Bitmap.createScaledBitmap(
                        flipped,
                        previewWidth,
                        previewHeight,
                        true,
                    )
                } else {
                    flipped
                }

            if (scaled !== flipped) {
                flipped.recycle()
            }

            AndroidPreviewBridge.publish(scaled)
        } catch (_: Throwable) {
            // Preview is diagnostic only.
        }
    }

    fun release() {
        if (!::handler.isInitialized) return

        val latch = CountDownLatch(1)

        handler.post {
            try {
                prepared.forEach { overlayRenderer?.delete(it.textureId) }
                prepared.clear()

                screenSurface?.release()
                cameraSurface?.release()
                screenTexture?.release()
                cameraTexture?.release()

                if (screenTextureId != 0) {
                    GLES20.glDeleteTextures(
                        1,
                        intArrayOf(screenTextureId),
                        0,
                    )
                }

                if (cameraTextureId != 0) {
                    GLES20.glDeleteTextures(
                        1,
                        intArrayOf(cameraTextureId),
                        0,
                    )
                }

                overlayRenderer?.release()
                renderer?.release()
                egl?.release()
            } finally {
                latch.countDown()
            }
        }

        latch.await(2, TimeUnit.SECONDS)
        thread.quitSafely()
    }
}
