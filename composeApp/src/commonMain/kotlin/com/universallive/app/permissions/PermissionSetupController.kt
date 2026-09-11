package com.universallive.app.permissions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class PermissionGrantState {
    UNKNOWN,
    GRANTED,
    DENIED,
    NOT_REQUIRED,
}

data class PermissionSetupSnapshot(
    val notifications: PermissionGrantState = PermissionGrantState.UNKNOWN,
    val microphone: PermissionGrantState = PermissionGrantState.UNKNOWN,
    val camera: PermissionGrantState = PermissionGrantState.UNKNOWN,
    val screenCaptureEducationComplete: Boolean = false,
)

class PermissionSetupController(
    private val requestNotificationsAction: () -> Unit = {},
    private val requestMicrophoneAction: () -> Unit = {},
    private val requestCameraAction: () -> Unit = {},
) {
    var snapshot: PermissionSetupSnapshot by mutableStateOf(PermissionSetupSnapshot())
        private set

    fun requestNotifications() = requestNotificationsAction()
    fun requestMicrophone() = requestMicrophoneAction()
    fun requestCamera() = requestCameraAction()

    fun updateNotifications(granted: Boolean, notRequired: Boolean = false) {
        snapshot = snapshot.copy(
            notifications = if (notRequired) PermissionGrantState.NOT_REQUIRED else if (granted) PermissionGrantState.GRANTED else PermissionGrantState.DENIED,
        )
    }

    fun updateMicrophone(granted: Boolean) {
        snapshot = snapshot.copy(
            microphone = if (granted) PermissionGrantState.GRANTED else PermissionGrantState.DENIED,
        )
    }

    fun updateCamera(granted: Boolean) {
        snapshot = snapshot.copy(
            camera = if (granted) PermissionGrantState.GRANTED else PermissionGrantState.DENIED,
        )
    }

    fun markScreenCaptureEducationComplete() {
        snapshot = snapshot.copy(screenCaptureEducationComplete = true)
    }
}
