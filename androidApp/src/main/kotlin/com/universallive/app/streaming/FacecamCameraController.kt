package com.universallive.app.streaming

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import androidx.core.content.ContextCompat

internal class FacecamCameraController(private val context: Context) {
    private var device: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var thread: HandlerThread? = null
    private var handler: Handler? = null

    @SuppressLint("MissingPermission")
    fun start(lensLabel: String, target: Surface, onStarted: () -> Unit = {}, onError: (String) -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            onError("Camera permission is required for facecam")
            return
        }
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val desired = if (lensLabel.equals("Back", true)) CameraCharacteristics.LENS_FACING_BACK else CameraCharacteristics.LENS_FACING_FRONT
        val cameraId = manager.cameraIdList.firstOrNull { id ->
            manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) == desired
        } ?: run { onError("Requested facecam lens is unavailable"); return }

        thread = HandlerThread("UniversalLiveCamera").also { it.start() }
        handler = Handler(thread!!.looper)
        manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                device = camera
                try {
                    camera.createCaptureSession(listOf(target), object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(s: CameraCaptureSession) {
                            session = s
                            try {
                                val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                                    addTarget(target)
                                    set(android.hardware.camera2.CaptureRequest.CONTROL_AF_MODE, android.hardware.camera2.CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO)
                                    set(android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE, android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE_ON)
                                }.build()
                                s.setRepeatingRequest(request, null, handler)
                                onStarted()
                            } catch (t: Throwable) { onError("Facecam capture failed: ${t.message}") }
                        }
                        override fun onConfigureFailed(s: CameraCaptureSession) { onError("Facecam camera session could not be configured") }
                    }, handler)
                } catch (t: Throwable) { onError("Facecam session failed: ${t.message}") }
            }
            override fun onDisconnected(camera: CameraDevice) { camera.close(); device = null }
            override fun onError(camera: CameraDevice, error: Int) { camera.close(); device = null; onError("Facecam camera error $error") }
        }, handler)
    }

    fun stop() {
        try { session?.stopRepeating() } catch (_: Throwable) {}
        try { session?.close() } catch (_: Throwable) {}
        try { device?.close() } catch (_: Throwable) {}
        session = null; device = null
        thread?.quitSafely(); thread = null; handler = null
    }
}
