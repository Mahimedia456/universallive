package com.universallive.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.universallive.app.integration.MobileBackendApi
import com.universallive.app.integration.MobileIntegrationState
import com.universallive.app.integration.OfflineMobileBackendApi
import com.universallive.app.navigation.AppRoute
import com.universallive.app.navigation.RootNavigation
import com.universallive.app.streaming.capture.CaptureController
import com.universallive.app.streaming.capture.CaptureStatus
import com.universallive.app.streaming.capture.PublishStatus
import com.universallive.app.streaming.connections.RtmpProfilesState
import com.universallive.app.streaming.facecam.FacecamState
import com.universallive.app.streaming.overlays.OverlayState
import com.universallive.app.streaming.overlays.SceneState
import com.universallive.app.streaming.state.StreamConfigState
import com.universallive.app.theme.UniversalLiveTheme

@Composable
fun UniversalLiveApp(
    captureController: CaptureController = CaptureController(),
    mobileBackendApi: MobileBackendApi = OfflineMobileBackendApi(),
    openLiveRequested: Boolean = false,
    onOpenLiveConsumed: () -> Unit = {},
) {
    UniversalLiveTheme {
        var route by remember { mutableStateOf<AppRoute>(AppRoute.Splash) }

        val streamState = remember { StreamConfigState() }
        val profilesState = remember { RtmpProfilesState() }
        val facecamState = remember { FacecamState() }
        val overlayState = remember { OverlayState() }
        val sceneState = remember { SceneState() }
        val integrationState = remember(mobileBackendApi) { MobileIntegrationState(mobileBackendApi) }

        val snapshot = captureController.snapshot
        val liveSessionActive =
            snapshot.status == CaptureStatus.STARTING ||
                snapshot.status == CaptureStatus.CAPTURING ||
                snapshot.publishStatus == PublishStatus.CONNECTING ||
                snapshot.publishStatus == PublishStatus.LIVE ||
                snapshot.publishStatus == PublishStatus.RECONNECTING

        fun isLiveWorkspaceRoute(candidate: AppRoute): Boolean = when (candidate) {
            AppRoute.LiveBroadcast,
            AppRoute.LiveControls,
            AppRoute.LiveSceneSwitcher,
            AppRoute.LiveChat,
            AppRoute.LiveStats,
            AppRoute.EndStreamConfirmation,
            AppRoute.LiveRecovery,
            AppRoute.NetworkDegraded,
            AppRoute.Reconnecting,
            AppRoute.DestinationFailure,
            AppRoute.StreamInterrupted -> true
            else -> false
        }

        fun navigate(candidate: AppRoute) {
            route = if (liveSessionActive && !isLiveWorkspaceRoute(candidate)) {
                AppRoute.LiveBroadcast
            } else {
                candidate
            }
        }

        // Returning to the app while Android's capture service is still publishing should always
        // reopen the active broadcast workspace instead of Home/Studio/Profile.
        LaunchedEffect(liveSessionActive, route) {
            if (
                liveSessionActive &&
                route != AppRoute.Splash &&
                !isLiveWorkspaceRoute(route)
            ) {
                route = AppRoute.LiveBroadcast
            }
        }

        // Notification taps explicitly request the active Live screen. During a cold Activity
        // recreation we first allow Splash to restore auth/backend state; the navigation guard
        // then lands on Live instead of Home.
        LaunchedEffect(openLiveRequested, liveSessionActive, route) {
            if (openLiveRequested && route != AppRoute.Splash) {
                if (liveSessionActive) route = AppRoute.LiveBroadcast
                onOpenLiveConsumed()
            } else if (openLiveRequested && !liveSessionActive) {
                onOpenLiveConsumed()
            }
        }

        Surface(modifier = Modifier.fillMaxSize()) {
            RootNavigation(
                route = route,
                streamState = streamState,
                profilesState = profilesState,
                captureController = captureController,
                facecamState = facecamState,
                overlayState = overlayState,
                sceneState = sceneState,
                integrationState = integrationState,
                onRouteChanged = ::navigate,
            )
        }
    }
}
