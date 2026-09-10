package com.universallive.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.universallive.app.navigation.AppRoute
import com.universallive.app.integration.MobileBackendApi
import com.universallive.app.integration.MobileIntegrationState
import com.universallive.app.integration.OfflineMobileBackendApi
import com.universallive.app.navigation.RootNavigation
import com.universallive.app.streaming.capture.CaptureController
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
) {
    UniversalLiveTheme {
        var route by remember { mutableStateOf<AppRoute>(AppRoute.Splash) }

        val streamState = remember { StreamConfigState() }
        val profilesState = remember { RtmpProfilesState() }
        val facecamState = remember { FacecamState() }
        val overlayState = remember { OverlayState() }
        val sceneState = remember { SceneState() }
        val integrationState = remember(mobileBackendApi) { MobileIntegrationState(mobileBackendApi) }

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
                onRouteChanged = { route = it },
            )
        }
    }
}
