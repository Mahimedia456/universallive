package com.universallive.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.universallive.app.integration.MobileBackendApi
import com.universallive.app.integration.MobileIntegrationState
import com.universallive.app.integration.OfflineMobileBackendApi
import com.universallive.app.navigation.AppDestination
import com.universallive.app.navigation.AppRoute
import com.universallive.app.navigation.RootNavigation
import com.universallive.app.permissions.PermissionSetupController
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
    permissionSetupController: PermissionSetupController = PermissionSetupController(),
    openLiveRequested: Boolean = false,
    onOpenLiveConsumed: () -> Unit = {},
    pushRouteRequested: String? = null,
    onPushRouteConsumed: () -> Unit = {},
    systemBackRequest: Int = 0,
    onRootBackRequested: () -> Unit = {},
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
        val nativeLiveSessionActive =
            snapshot.status == CaptureStatus.STARTING ||
                snapshot.status == CaptureStatus.CAPTURING ||
                snapshot.publishStatus == PublishStatus.CONNECTING ||
                snapshot.publishStatus == PublishStatus.LIVE ||
                snapshot.publishStatus == PublishStatus.RECONNECTING
        val liveSessionActive = nativeLiveSessionActive || integrationState.hasActuallyLiveBroadcast

        fun isLiveWorkspaceRoute(candidate: AppRoute): Boolean = when (candidate) {
            AppRoute.LiveBroadcast,
            AppRoute.LiveControls,
            AppRoute.LiveSceneSwitcher,
            AppRoute.LiveChat,
            AppRoute.LiveStats,
            AppRoute.LiveHealth,
            AppRoute.LiveDestinations,
            AppRoute.EndStreamConfirmation,
            AppRoute.EndingStream,
            AppRoute.LiveRecovery,
            AppRoute.NetworkDegraded,
            AppRoute.Reconnecting,
            AppRoute.StreamRecovered,
            AppRoute.RecoveryFailed,
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

        // Only an actual native publisher or a backend session already marked live/reconnecting
        // is allowed to force the app into the Live workspace. Draft/created/connecting sessions
        // never hijack normal app startup.
        LaunchedEffect(liveSessionActive, route) {
            if (
                liveSessionActive &&
                route != AppRoute.Splash &&
                !isLiveWorkspaceRoute(route)
            ) {
                route = AppRoute.LiveBroadcast
            }
        }

        LaunchedEffect(openLiveRequested, liveSessionActive, route) {
            if (openLiveRequested && route != AppRoute.Splash) {
                if (liveSessionActive) route = AppRoute.LiveBroadcast
                onOpenLiveConsumed()
            } else if (openLiveRequested && !liveSessionActive) {
                onOpenLiveConsumed()
            }
        }

        LaunchedEffect(pushRouteRequested, integrationState.isAuthenticated, route) {
            val requested = pushRouteRequested?.trim()?.lowercase() ?: return@LaunchedEffect
            if (route == AppRoute.Splash || !integrationState.isAuthenticated) return@LaunchedEffect
            route = when (requested) {
                "live" -> if (liveSessionActive) AppRoute.LiveBroadcast else AppRoute.Main(AppDestination.Home)
                "billing" -> AppRoute.Billing
                "profile" -> AppRoute.Main(AppDestination.Settings)
                "activity", "history" -> AppRoute.Main(AppDestination.Activity)
                "studio", "scenes" -> AppRoute.Main(AppDestination.Scenes)
                else -> AppRoute.Notifications
            }
            onPushRouteConsumed()
        }

        // Android system Back follows the same product hierarchy as the visible back buttons.
        // Root screens intentionally delegate to MainActivity, which requires a second Back press
        // within a short window before closing the Activity.
        LaunchedEffect(systemBackRequest) {
            if (systemBackRequest <= 0 || route == AppRoute.Splash) return@LaunchedEffect
            val target = systemBackTarget(route)
            if (target == null) onRootBackRequested() else navigate(target)
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
                permissionSetupController = permissionSetupController,
                onRouteChanged = ::navigate,
            )
        }
    }
}

private fun systemBackTarget(route: AppRoute): AppRoute? = when (route) {
    AppRoute.Splash,
    AppRoute.Welcome,
    AppRoute.AccountCreatedSuccess,
    AppRoute.LiveBroadcast -> null

    AppRoute.SignIn,
    AppRoute.CreateAccount -> AppRoute.Welcome
    AppRoute.ForgotPassword -> AppRoute.SignIn
    is AppRoute.VerifyEmail -> if (route.flow == com.universallive.app.navigation.VerificationFlow.AccountCreation) AppRoute.CreateAccount else AppRoute.ForgotPassword
    is AppRoute.CodeExpired -> AppRoute.VerifyEmail(route.flow)
    AppRoute.CreateNewPassword -> AppRoute.ForgotPassword
    AppRoute.PasswordResetSuccess -> AppRoute.SignIn

    // First-time creator setup stays inside onboarding. It never falls back to the
    // post-email-verification success screen when Android Back is pressed.
    AppRoute.CreatorSetup -> null
    AppRoute.CreatorContentType -> AppRoute.CreatorSetup
    AppRoute.CreatorPlatforms -> AppRoute.CreatorContentType
    AppRoute.CreatorExperience -> AppRoute.CreatorPlatforms
    AppRoute.CreatorGoal -> AppRoute.CreatorExperience
    AppRoute.CreatorSetupComplete -> AppRoute.CreatorGoal
    AppRoute.PermissionHub -> AppRoute.CreatorSetupComplete
    AppRoute.NotificationPermission -> AppRoute.PermissionHub
    AppRoute.MicrophonePermission -> AppRoute.NotificationPermission
    AppRoute.CameraPermission -> AppRoute.MicrophonePermission
    AppRoute.ScreenCaptureEducation -> AppRoute.CameraPermission
    AppRoute.PermissionsComplete -> AppRoute.ScreenCaptureEducation
    AppRoute.MicCameraPermission -> AppRoute.PermissionHub
    AppRoute.StudioReady -> AppRoute.PermissionHub

    AppRoute.StreamReadiness -> AppRoute.Main(AppDestination.Home)
    AppRoute.QuickGoLive -> AppRoute.Main(AppDestination.Home)
    AppRoute.Notifications -> AppRoute.Main(AppDestination.Home)
    AppRoute.NotificationDetail -> AppRoute.Notifications

    AppRoute.Connections -> AppRoute.Main(AppDestination.Home)
    AppRoute.AddConnection -> AppRoute.Connections
    AppRoute.PlatformAuthorization -> AppRoute.AddConnection
    AppRoute.ChannelPicker -> AppRoute.PlatformAuthorization
    AppRoute.CustomRtmp -> AppRoute.AddConnection
    AppRoute.ConnectionDetail -> AppRoute.Connections
    AppRoute.EditConnection,
    AppRoute.ConnectionTest,
    AppRoute.DisconnectConfirmation,
    AppRoute.ConnectionTroubleshooting -> AppRoute.ConnectionDetail

    AppRoute.SceneLibrary -> AppRoute.Main(AppDestination.Scenes)
    AppRoute.CreateScene,
    AppRoute.SceneTemplates,
    AppRoute.SceneOptions -> AppRoute.SceneLibrary
    AppRoute.SceneEditor -> AppRoute.SceneLibrary
    AppRoute.AddSource,
    AppRoute.Layers,
    AppRoute.TransformInspector,
    AppRoute.SourceProperties -> AppRoute.SceneEditor
    AppRoute.FacecamEditor,
    AppRoute.AudioMixer,
    AppRoute.QualityCenter -> AppRoute.Main(AppDestination.Scenes)
    AppRoute.ImageLogoEditor,
    AppRoute.TextEditor,
    AppRoute.BrowserSourceEditor,
    AppRoute.BackgroundEditor,
    AppRoute.ChatOverlayEditor,
    AppRoute.AlertEditor,
    AppRoute.GoalOverlayEditor -> AppRoute.AddSource
    AppRoute.AudioAdvanced -> AppRoute.AudioMixer

    AppRoute.StreamDetails -> AppRoute.Main(AppDestination.GoLive)
    AppRoute.DestinationSelection -> AppRoute.StreamDetails
    AppRoute.StreamInfo -> AppRoute.DestinationSelection
    AppRoute.StreamQuality -> AppRoute.StreamInfo
    AppRoute.AudioCameraSetup -> AppRoute.StreamQuality
    AppRoute.SetupReview -> AppRoute.AudioCameraSetup
    AppRoute.Preflight -> AppRoute.SetupReview
    AppRoute.PreflightNetwork -> AppRoute.Preflight
    AppRoute.PreflightDevices -> AppRoute.PreflightNetwork
    AppRoute.PreflightDestinations -> AppRoute.PreflightDevices
    AppRoute.PreflightSuccess -> AppRoute.PreflightDestinations
    AppRoute.Countdown -> AppRoute.PreflightSuccess

    AppRoute.LiveControls,
    AppRoute.LiveSceneSwitcher,
    AppRoute.LiveChat,
    AppRoute.LiveStats,
    AppRoute.LiveHealth,
    AppRoute.LiveDestinations,
    AppRoute.LiveRecovery,
    AppRoute.NetworkDegraded,
    AppRoute.StreamRecovered,
    AppRoute.DestinationFailure,
    AppRoute.StreamInterrupted -> AppRoute.LiveBroadcast
    AppRoute.EndStreamConfirmation -> AppRoute.LiveBroadcast
    AppRoute.Reconnecting,
    AppRoute.RecoveryFailed -> AppRoute.LiveRecovery
    AppRoute.EndingStream -> AppRoute.EndStreamConfirmation

    AppRoute.StreamProcessing -> AppRoute.Main(AppDestination.Home)
    AppRoute.StreamSummary -> AppRoute.Main(AppDestination.Activity)
    AppRoute.StreamPerformance -> AppRoute.StreamSummary
    AppRoute.ActivityDetail -> AppRoute.Main(AppDestination.Activity)

    AppRoute.Plans -> AppRoute.Main(AppDestination.Settings)
    AppRoute.PlanComparison -> AppRoute.Plans
    AppRoute.CheckoutConfirmation,
    AppRoute.PurchaseState,
    AppRoute.ManageSubscription,
    AppRoute.Billing,
    AppRoute.PaymentMethods -> AppRoute.Main(AppDestination.Settings)
    AppRoute.EditProfile,
    AppRoute.SettingsHub,
    AppRoute.StreamingSettings,
    AppRoute.AccountSettings,
    AppRoute.AccountSecurity,
    AppRoute.StreamingDefaults,
    AppRoute.VideoAudioDefaults,
    AppRoute.AppearanceNotifications -> AppRoute.Main(AppDestination.Settings)

    AppRoute.HelpCenter,
    AppRoute.Diagnostics,
    AppRoute.LegalPrivacy -> AppRoute.SettingsHub
    AppRoute.Troubleshooting,
    AppRoute.ContactSupport -> AppRoute.HelpCenter
    AppRoute.DeleteAccount -> AppRoute.AccountSecurity
    AppRoute.GlobalComponents,
    AppRoute.DesignSystemStates,
    AppRoute.FunctionalQa -> AppRoute.Diagnostics

    AppRoute.Offline,
    AppRoute.ServiceError -> AppRoute.Main(AppDestination.Home)
    AppRoute.SessionExpired -> AppRoute.SignIn
    AppRoute.PermissionBlocked -> AppRoute.SettingsHub

    is AppRoute.Main -> if (route.destination == AppDestination.Home) null else AppRoute.Main(AppDestination.Home)
}
