package com.universallive.app.navigation

import androidx.compose.runtime.Composable
import com.universallive.app.features.activity.ActivityScreen
import com.universallive.app.features.auth.*
import com.universallive.app.features.batch2.*
import com.universallive.app.features.batch3.*
import com.universallive.app.features.batch4.*
import com.universallive.app.features.batch5.*
import com.universallive.app.features.batch6.*
import com.universallive.app.features.golive.GoLiveScreen
import com.universallive.app.features.onboarding.*
import com.universallive.app.features.overlays.OverlaysScreen
import com.universallive.app.features.scenes.ScenesScreen
import com.universallive.app.features.settings.SettingsScreen
import com.universallive.app.streaming.capture.CaptureController
import com.universallive.app.streaming.connections.RtmpProfilesState
import com.universallive.app.streaming.facecam.FacecamState
import com.universallive.app.streaming.overlays.OverlayState
import com.universallive.app.streaming.overlays.SceneState
import com.universallive.app.streaming.state.StreamConfigState

@Composable
fun RootNavigation(
    route: AppRoute,
    streamState: StreamConfigState,
    profilesState: RtmpProfilesState,
    captureController: CaptureController,
    facecamState: FacecamState,
    overlayState: OverlayState,
    sceneState: SceneState,
    onRouteChanged: (AppRoute) -> Unit,
) {
    val goMain: (AppDestination) -> Unit = { destination ->
        onRouteChanged(AppRoute.Main(destination))
    }
    val home = { onRouteChanged(AppRoute.Main(AppDestination.Home)) }
    val connections = { onRouteChanged(AppRoute.Connections) }

    when (route) {
        AppRoute.Splash -> SplashScreen { onRouteChanged(AppRoute.Welcome) }
        AppRoute.Welcome -> WelcomeScreen(onRouteChanged)
        AppRoute.SignIn -> SignInScreen(onRouteChanged)
        AppRoute.CreateAccount -> CreateAccountScreen(onRouteChanged)
        AppRoute.ForgotPassword -> ForgotPasswordScreen(onRouteChanged)
        is AppRoute.VerifyEmail -> VerifyEmailScreen(route.flow, onRouteChanged)
        is AppRoute.CodeExpired -> CodeExpiredScreen(route.flow, onRouteChanged)
        AppRoute.CreateNewPassword -> CreateNewPasswordScreen(onRouteChanged)
        AppRoute.PasswordResetSuccess -> PasswordResetSuccessScreen(onRouteChanged)
        AppRoute.AccountCreatedSuccess -> AccountCreatedSuccessScreen(onRouteChanged)
        AppRoute.CreatorSetup -> CreatorSetupScreen(onRouteChanged)
        AppRoute.PermissionHub -> PermissionHubScreen(onRouteChanged)
        AppRoute.MicCameraPermission -> MicCameraPermissionScreen(onRouteChanged)
        AppRoute.ScreenCaptureEducation -> ScreenCaptureEducationScreen(onRouteChanged)
        AppRoute.StudioReady -> StudioReadyScreen(onRouteChanged)

        AppRoute.StreamReadiness -> StreamReadinessScreen(home)
        AppRoute.QuickGoLive -> QuickGoLiveScreen(home)
        AppRoute.Notifications -> NotificationsScreen(home) { onRouteChanged(AppRoute.NotificationDetail) }
        AppRoute.NotificationDetail -> NotificationDetailScreen { onRouteChanged(AppRoute.Notifications) }

        AppRoute.Connections -> ConnectionsV2Screen(onRouteChanged, home)
        AppRoute.AddConnection -> AddConnectionScreen(onRouteChanged, connections)
        AppRoute.PlatformAuthorization -> PlatformAuthorizationScreen(onRouteChanged) { onRouteChanged(AppRoute.AddConnection) }
        AppRoute.ChannelPicker -> ChannelPickerScreen { onRouteChanged(AppRoute.PlatformAuthorization) }
        AppRoute.CustomRtmp -> CustomRtmpScreen(connections)

        AppRoute.ConnectionDetail -> ConnectionDetailScreen(onRouteChanged, connections)
        AppRoute.EditConnection -> EditConnectionScreen { onRouteChanged(AppRoute.ConnectionDetail) }
        AppRoute.ConnectionTest -> ConnectionTestScreen { onRouteChanged(AppRoute.ConnectionDetail) }
        AppRoute.DisconnectConfirmation -> DisconnectConfirmationScreen { onRouteChanged(AppRoute.ConnectionDetail) }
        AppRoute.ConnectionTroubleshooting -> ConnectionTroubleshootingScreen { onRouteChanged(AppRoute.ConnectionDetail) }

        AppRoute.SceneLibrary -> SceneLibraryV2Screen(
            sceneState = sceneState,
            onRoute = onRouteChanged,
            onBack = { onRouteChanged(AppRoute.Main(AppDestination.Scenes)) },
        )
        AppRoute.CreateScene -> CreateSceneScreen(
            sceneState = sceneState,
            onBack = { onRouteChanged(AppRoute.SceneLibrary) },
            onCreated = { onRouteChanged(AppRoute.SceneEditor) },
        )
        AppRoute.SceneTemplates -> SceneTemplatesScreen { onRouteChanged(AppRoute.SceneLibrary) }
        AppRoute.SceneOptions -> SceneOptionsScreen(
            onRoute = onRouteChanged,
            onBack = { onRouteChanged(AppRoute.SceneLibrary) },
        )

        AppRoute.SceneEditor -> SceneEditorV2Screen(
            sceneState = sceneState,
            onRoute = onRouteChanged,
            onBack = { onRouteChanged(AppRoute.Main(AppDestination.Scenes)) },
        )
        AppRoute.AddSource -> AddSourceScreen(
            onRoute = onRouteChanged,
            onBack = { onRouteChanged(AppRoute.SceneEditor) },
        )
        AppRoute.Layers -> LayersScreen(
            onRoute = onRouteChanged,
            onBack = { onRouteChanged(AppRoute.SceneEditor) },
        )
        AppRoute.TransformInspector -> TransformInspectorScreen { onRouteChanged(AppRoute.SceneEditor) }
        AppRoute.SourceProperties -> SourcePropertiesScreen { onRouteChanged(AppRoute.SceneEditor) }

        AppRoute.FacecamEditor -> FacecamEditorScreen { onRouteChanged(AppRoute.AddSource) }
        AppRoute.ImageLogoEditor -> ImageLogoEditorScreen { onRouteChanged(AppRoute.AddSource) }
        AppRoute.TextEditor -> TextEditorScreen { onRouteChanged(AppRoute.AddSource) }
        AppRoute.BrowserSourceEditor -> BrowserSourceEditorScreen { onRouteChanged(AppRoute.AddSource) }
        AppRoute.BackgroundEditor -> BackgroundEditorScreen { onRouteChanged(AppRoute.AddSource) }

        AppRoute.ChatOverlayEditor -> ChatOverlayEditorScreen { onRouteChanged(AppRoute.AddSource) }
        AppRoute.AlertEditor -> AlertEditorScreen { onRouteChanged(AppRoute.AddSource) }
        AppRoute.GoalOverlayEditor -> GoalOverlayEditorScreen { onRouteChanged(AppRoute.AddSource) }
        AppRoute.AudioMixer -> AudioMixerV2Screen(
            microphoneEnabled = streamState.config.microphoneEnabled,
            deviceAudioEnabled = streamState.config.internalAudioEnabled,
            onMicrophoneChanged = streamState::setMicrophoneEnabled,
            onDeviceAudioChanged = streamState::setInternalAudioEnabled,
            onAdvanced = { onRouteChanged(AppRoute.AudioAdvanced) },
            onBack = { onRouteChanged(AppRoute.Main(AppDestination.Scenes)) },
        )
        AppRoute.AudioAdvanced -> AudioAdvancedScreen { onRouteChanged(AppRoute.AudioMixer) }

        AppRoute.StreamDetails -> StreamDetailsV2Screen(
            sceneState = sceneState,
            onRoute = onRouteChanged,
            onDestination = goMain,
        )
        AppRoute.DestinationSelection -> DestinationSelectionScreen(
            profilesState = profilesState,
            onRoute = onRouteChanged,
            onBack = { onRouteChanged(AppRoute.StreamDetails) },
        )
        AppRoute.StreamQuality -> StreamQualityV2Screen(
            streamState = streamState,
            onRoute = onRouteChanged,
            onBack = { onRouteChanged(AppRoute.DestinationSelection) },
        )
        AppRoute.Preflight -> PreflightV2Screen(
            streamState = streamState,
            profilesState = profilesState,
            sceneState = sceneState,
            captureController = captureController,
            onRoute = onRouteChanged,
            onBack = { onRouteChanged(AppRoute.StreamQuality) },
        )
        AppRoute.Countdown -> CountdownScreen(
            streamState = streamState,
            profilesState = profilesState,
            captureController = captureController,
            facecamState = facecamState,
            overlayState = overlayState,
            sceneState = sceneState,
            onLive = { onRouteChanged(AppRoute.LiveBroadcast) },
            onCancel = { onRouteChanged(AppRoute.Preflight) },
        )

        AppRoute.LiveBroadcast -> LiveBroadcastV2Screen(
            streamState = streamState,
            captureController = captureController,
            facecamState = facecamState,
            sceneState = sceneState,
            onRoute = onRouteChanged,
            onDestination = goMain,
        )
        AppRoute.LiveControls -> LiveControlsScreen(
            streamState = streamState,
            captureController = captureController,
            facecamState = facecamState,
            onRoute = onRouteChanged,
            onBack = { onRouteChanged(AppRoute.LiveBroadcast) },
        )
        AppRoute.LiveSceneSwitcher -> LiveSceneSwitcherScreen(
            sceneState = sceneState,
            captureController = captureController,
            onBack = { onRouteChanged(AppRoute.LiveBroadcast) },
        )
        AppRoute.LiveChat -> LiveChatScreen { onRouteChanged(AppRoute.LiveBroadcast) }
        AppRoute.LiveStats -> LiveStatsV2Screen(
            streamState = streamState,
            captureController = captureController,
            onBack = { onRouteChanged(AppRoute.LiveBroadcast) },
        )
        AppRoute.EndStreamConfirmation -> EndStreamConfirmationScreen(
            captureController = captureController,
            onContinue = { onRouteChanged(AppRoute.LiveBroadcast) },
            onEnded = { onRouteChanged(AppRoute.StreamProcessing) },
        )

        AppRoute.LiveRecovery -> ResilienceHubScreen(
            captureController = captureController,
            onNetwork = { onRouteChanged(AppRoute.NetworkDegraded) },
            onReconnect = { onRouteChanged(AppRoute.Reconnecting) },
            onDestinationFailure = { onRouteChanged(AppRoute.DestinationFailure) },
            onInterrupted = { onRouteChanged(AppRoute.StreamInterrupted) },
            onBack = { onRouteChanged(AppRoute.LiveBroadcast) },
        )
        AppRoute.NetworkDegraded -> NetworkDegradedScreen(captureController) { onRouteChanged(AppRoute.LiveBroadcast) }
        AppRoute.Reconnecting -> ReconnectingScreen(captureController) { onRouteChanged(AppRoute.LiveBroadcast) }
        AppRoute.DestinationFailure -> DestinationFailureScreen { onRouteChanged(AppRoute.LiveBroadcast) }
        AppRoute.StreamInterrupted -> StreamInterruptedScreen(captureController) { onRouteChanged(AppRoute.LiveBroadcast) }

        AppRoute.StreamProcessing -> StreamProcessingScreen(
            onSummary = { onRouteChanged(AppRoute.StreamSummary) },
            onHome = { onRouteChanged(AppRoute.Main(AppDestination.Home)) },
        )
        AppRoute.StreamSummary -> StreamSummaryScreen(
            captureController = captureController,
            onPerformance = { onRouteChanged(AppRoute.StreamPerformance) },
            onDone = { onRouteChanged(AppRoute.Main(AppDestination.Activity)) },
        )
        AppRoute.StreamPerformance -> StreamPerformanceScreen(captureController) { onRouteChanged(AppRoute.StreamSummary) }
        AppRoute.ActivityDetail -> ActivityDetailScreen { onRouteChanged(AppRoute.Main(AppDestination.Activity)) }

        AppRoute.Plans -> PlansScreen(
            onRoute = onRouteChanged,
            onBack = { onRouteChanged(AppRoute.Main(AppDestination.Home)) },
        )
        AppRoute.PlanComparison -> PlanComparisonScreen { onRouteChanged(AppRoute.Plans) }
        AppRoute.CheckoutConfirmation -> CheckoutConfirmationScreen(
            onRoute = onRouteChanged,
            onBack = { onRouteChanged(AppRoute.Plans) },
        )
        AppRoute.PurchaseState -> PurchaseStateScreen(
            onManage = { onRouteChanged(AppRoute.ManageSubscription) },
            onBack = { onRouteChanged(AppRoute.Plans) },
        )
        AppRoute.ManageSubscription -> ManageSubscriptionScreen { onRouteChanged(AppRoute.Plans) }

        AppRoute.AccountSecurity -> AccountSecurityScreen(
            onRoute = onRouteChanged,
            onBack = { onRouteChanged(AppRoute.Main(AppDestination.Settings)) },
        )
        AppRoute.StreamingDefaults -> StreamingDefaultsScreen(
            streamState = streamState,
            profilesState = profilesState,
            onBack = { onRouteChanged(AppRoute.Main(AppDestination.Settings)) },
        )
        AppRoute.VideoAudioDefaults -> VideoAudioDefaultsScreen(
            streamState = streamState,
            onBack = { onRouteChanged(AppRoute.Main(AppDestination.Settings)) },
        )
        AppRoute.AppearanceNotifications -> AppearanceNotificationsScreen {
            onRouteChanged(AppRoute.Main(AppDestination.Settings))
        }

        AppRoute.HelpCenter -> HelpCenterScreen(
            onRoute = onRouteChanged,
            onBack = { onRouteChanged(AppRoute.Main(AppDestination.Settings)) },
        )
        AppRoute.Troubleshooting -> TroubleshootingScreen { onRouteChanged(AppRoute.HelpCenter) }
        AppRoute.ContactSupport -> ContactSupportScreen { onRouteChanged(AppRoute.HelpCenter) }
        AppRoute.LegalPrivacy -> LegalPrivacyScreen { onRouteChanged(AppRoute.HelpCenter) }
        AppRoute.DeleteAccount -> DeleteAccountScreen { onRouteChanged(AppRoute.AccountSecurity) }

        AppRoute.Offline -> OfflineScreen(
            onRetry = { onRouteChanged(AppRoute.Main(AppDestination.Home)) },
            onHome = { onRouteChanged(AppRoute.Main(AppDestination.Home)) },
        )
        AppRoute.SessionExpired -> SessionExpiredScreen { onRouteChanged(AppRoute.SignIn) }
        AppRoute.PermissionBlocked -> PermissionBlockedScreen { onRouteChanged(AppRoute.Main(AppDestination.Settings)) }
        AppRoute.ServiceError -> ServiceErrorScreen(
            onRetry = { onRouteChanged(AppRoute.Main(AppDestination.Home)) },
            onHome = { onRouteChanged(AppRoute.Main(AppDestination.Home)) },
        )
        AppRoute.DesignSystemStates -> DesignSystemStatesScreen {
            onRouteChanged(AppRoute.Main(AppDestination.Settings))
        }

        is AppRoute.Main -> when (route.destination) {
            AppDestination.Home -> HomeV2Screen(onRouteChanged, goMain)
            AppDestination.Scenes -> StudioHomeV2Screen(sceneState, onRouteChanged, goMain)
            AppDestination.GoLive -> StreamDetailsV2Screen(
                sceneState = sceneState,
                onRoute = onRouteChanged,
                onDestination = goMain,
            )
            AppDestination.Activity -> ActivityV2Screen(onRouteChanged, goMain)
            AppDestination.Settings -> ProfileV2Screen(onRouteChanged, goMain)
            AppDestination.Overlays -> OverlaysScreen(overlayState, goMain)
            AppDestination.Connections -> ConnectionsV2Screen(onRouteChanged, home)
        }
    }
}
