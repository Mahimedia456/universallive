package com.universallive.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.media.projection.MediaProjectionConfig
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.universallive.app.streaming.CaptureBridge
import com.universallive.app.integration.AndroidMobileBackendApi
import com.universallive.app.streaming.ScreenCaptureService
import com.universallive.app.streaming.capture.CaptureController
import com.universallive.app.streaming.capture.CaptureMode
import com.universallive.app.permissions.PermissionSetupController

class MainActivity : ComponentActivity() {
    private lateinit var captureController: CaptureController
    private lateinit var permissionSetupController: PermissionSetupController
    private var openLiveRequested by mutableStateOf(false)
    private var pushRouteRequested by mutableStateOf<String?>(null)
    private var systemBackRequest by mutableStateOf(0)
    private var lastRootBackAt: Long = 0L

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionSetupController.updateNotifications(granted)
    }

    private val microphonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionSetupController.updateMicrophone(granted)
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionSetupController.updateCamera(granted)
    }

    private val capturePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val data = result.data
        if (result.resultCode == RESULT_OK && data != null) {
            val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                action = ScreenCaptureService.ACTION_START
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data)
                putExtra(ScreenCaptureService.EXTRA_CAPTURE_MIC, captureController.requestedMicrophone)
                putExtra(ScreenCaptureService.EXTRA_CAPTURE_INTERNAL_AUDIO, captureController.requestedInternalAudio)
                putExtra(ScreenCaptureService.EXTRA_VIDEO_WIDTH, captureController.requestedWidth)
                putExtra(ScreenCaptureService.EXTRA_VIDEO_HEIGHT, captureController.requestedHeight)
                putExtra(ScreenCaptureService.EXTRA_VIDEO_FPS, captureController.requestedFps)
                putExtra(ScreenCaptureService.EXTRA_VIDEO_BITRATE_KBPS, captureController.requestedBitrateKbps)
                putExtra(ScreenCaptureService.EXTRA_VIDEO_ORIENTATION, captureController.requestedOrientation)
                putExtra(ScreenCaptureService.EXTRA_RTMP_SERVER_URL, captureController.requestedRtmpServerUrl)
                putExtra(ScreenCaptureService.EXTRA_STREAM_KEY, captureController.requestedStreamKey)
                putExtra(ScreenCaptureService.EXTRA_TARGET_NAME, captureController.requestedTargetName)
                putExtra(ScreenCaptureService.EXTRA_FACECAM_ENABLED, captureController.requestedFacecamEnabled)
                putExtra(ScreenCaptureService.EXTRA_FACECAM_LENS, captureController.requestedFacecamLens)
                putExtra(ScreenCaptureService.EXTRA_FACECAM_SHAPE, captureController.requestedFacecamShape)
                putExtra(ScreenCaptureService.EXTRA_FACECAM_X, captureController.requestedFacecamX)
                putExtra(ScreenCaptureService.EXTRA_FACECAM_Y, captureController.requestedFacecamY)
                putExtra(ScreenCaptureService.EXTRA_FACECAM_SIZE, captureController.requestedFacecamSize)
                putExtra(ScreenCaptureService.EXTRA_FACECAM_MIRRORED, captureController.requestedFacecamMirrored)
                putExtra(ScreenCaptureService.EXTRA_SCENE_NAME, captureController.requestedSceneName)
                putExtra(ScreenCaptureService.EXTRA_OVERLAY_PAYLOAD, captureController.requestedOverlayPayload)
                putExtra(ScreenCaptureService.EXTRA_CAPTURE_MODE, captureController.requestedCaptureMode.label)
            }
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            captureController.permissionDenied("Screen-capture permission was not granted")
        }
    }

    private val runtimePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val micOk = !captureController.requestedMicrophone || grants[Manifest.permission.RECORD_AUDIO] != false
        val cameraOk = !captureController.requestedFacecamEnabled || grants[Manifest.permission.CAMERA] != false
        if (micOk && cameraOk) {
            launchMediaProjectionConsent()
        } else {
            captureController.permissionDenied(
                when {
                    !cameraOk -> "Camera permission was not granted for facecam"
                    else -> "Microphone/audio permission was not granted"
                },
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openLiveRequested = intent?.getBooleanExtra(EXTRA_OPEN_LIVE_SCREEN, false) == true
        pushRouteRequested = intent?.getStringExtra(EXTRA_PUSH_ROUTE)

        permissionSetupController = PermissionSetupController(
            requestNotificationsAction = {
                if (Build.VERSION.SDK_INT < 33) {
                    permissionSetupController.updateNotifications(granted = true, notRequired = true)
                } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    permissionSetupController.updateNotifications(granted = true)
                } else {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            requestMicrophoneAction = {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    permissionSetupController.updateMicrophone(granted = true)
                } else {
                    microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            requestCameraAction = {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    permissionSetupController.updateCamera(granted = true)
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
        )
        syncPermissionSetupState()

        captureController = CaptureController(
            requestStart = { requestCapturePermissions() },
            requestStop = {
                startService(Intent(this, ScreenCaptureService::class.java).apply {
                    action = ScreenCaptureService.ACTION_STOP
                })
            },
            requestSceneUpdate = { sceneName, payload ->
                startService(Intent(this, ScreenCaptureService::class.java).apply {
                    action = ScreenCaptureService.ACTION_UPDATE_SCENE
                    putExtra(ScreenCaptureService.EXTRA_SCENE_NAME, sceneName)
                    putExtra(ScreenCaptureService.EXTRA_OVERLAY_PAYLOAD, payload)
                })
            },
            requestBitrateUpdate = { bitrateKbps ->
                startService(Intent(this, ScreenCaptureService::class.java).apply {
                    action = ScreenCaptureService.ACTION_UPDATE_BITRATE
                    putExtra(ScreenCaptureService.EXTRA_VIDEO_BITRATE_KBPS, bitrateKbps)
                })
            },
        )

        CaptureBridge.listener = { snapshot ->
            runOnUiThread { captureController.update(snapshot) }
        }

        setContent {
            BackHandler(enabled = true) { systemBackRequest += 1 }
            UniversalLiveApp(
                captureController = captureController,
                mobileBackendApi = AndroidMobileBackendApi(applicationContext),
                permissionSetupController = permissionSetupController,
                openLiveRequested = openLiveRequested,
                onOpenLiveConsumed = { openLiveRequested = false },
                pushRouteRequested = pushRouteRequested,
                onPushRouteConsumed = { pushRouteRequested = null },
                systemBackRequest = systemBackRequest,
                onRootBackRequested = ::handleRootBackRequested,
            )
        }
    }

    private fun handleRootBackRequested() {
        val now = System.currentTimeMillis()
        if (now - lastRootBackAt <= 1800L) {
            finish()
            return
        }
        lastRootBackAt = now
        Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_LIVE_SCREEN, false)) {
            openLiveRequested = true
        }
        intent.getStringExtra(EXTRA_PUSH_ROUTE)?.takeIf { it.isNotBlank() }?.let {
            pushRouteRequested = it
        }
    }

    private fun syncPermissionSetupState() {
        permissionSetupController.updateMicrophone(
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED,
        )
        permissionSetupController.updateCamera(
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
        if (Build.VERSION.SDK_INT < 33) {
            permissionSetupController.updateNotifications(granted = true, notRequired = true)
        } else {
            permissionSetupController.updateNotifications(
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
            )
        }
    }

    private fun requestCapturePermissions() {
        val permissions = mutableListOf<String>()
        val needsAudioPermission = captureController.requestedMicrophone || captureController.requestedInternalAudio
        if (needsAudioPermission && Build.VERSION.SDK_INT >= 23 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        ) permissions += Manifest.permission.RECORD_AUDIO

        if (captureController.requestedFacecamEnabled && Build.VERSION.SDK_INT >= 23 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        ) permissions += Manifest.permission.CAMERA

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) permissions += Manifest.permission.POST_NOTIFICATIONS

        if (permissions.isNotEmpty()) runtimePermissionsLauncher.launch(permissions.toTypedArray())
        else launchMediaProjectionConsent()
    }

    private fun launchMediaProjectionConsent() {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = if (Build.VERSION.SDK_INT >= 34) {
            val config = when (captureController.requestedCaptureMode) {
                CaptureMode.ENTIRE_DEVICE -> MediaProjectionConfig.createConfigForDefaultDisplay()
                CaptureMode.USER_CHOICE -> MediaProjectionConfig.createConfigForUserChoice()
            }
            manager.createScreenCaptureIntent(config)
        } else {
            manager.createScreenCaptureIntent()
        }
        capturePermissionLauncher.launch(captureIntent)
    }

    override fun onDestroy() {
        CaptureBridge.listener = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_OPEN_LIVE_SCREEN = "open_live_screen"
        const val EXTRA_PUSH_ROUTE = "push_route"
        const val EXTRA_PUSH_NOTIFICATION_ID = "push_notification_id"
    }

}
