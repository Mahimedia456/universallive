package com.universallive.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import com.universallive.app.features.account.*
import com.universallive.app.features.auth.*
import com.universallive.app.features.batch2.*
import com.universallive.app.features.batch3.*
import com.universallive.app.features.batch4.*
import com.universallive.app.features.batch5.*
import com.universallive.app.features.batch6.*
import com.universallive.app.features.integration.*
import com.universallive.app.features.home.ConnectedHomeScreen
import com.universallive.app.features.home.Phase09StreamReadinessScreen
import com.universallive.app.features.live.*
import com.universallive.app.features.connections.*
import com.universallive.app.integration.MobileIntegrationState
import com.universallive.app.permissions.PermissionSetupController
import com.universallive.app.features.onboarding.*
import com.universallive.app.features.studio.*
import com.universallive.app.features.system.*
import com.universallive.app.features.welcome.WelcomeScreen
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
    integrationState: MobileIntegrationState,
    permissionSetupController: PermissionSetupController,
    onRouteChanged: (AppRoute) -> Unit,
) {
    val goMain: (AppDestination) -> Unit = { destination ->
        onRouteChanged(AppRoute.Main(destination))
    }
    val home = { onRouteChanged(AppRoute.Main(AppDestination.Home)) }
    val connections = { onRouteChanged(AppRoute.Connections) }

    when (route) {
        AppRoute.Splash -> {
            LaunchedEffect(Unit) {
                // Restore the account in parallel while keeping the approved branded splash
                // visible long enough to feel deliberate instead of flashing for one frame.
                val restored = coroutineScope {
                    val restoreTask = async { integrationState.restore() }
                    delay(250)
                    restoreTask.await()
                }

                onRouteChanged(
                    when {
                        integrationState.systemState?.maintenanceEnabled == true -> AppRoute.ServiceError
                        restored && integrationState.hasActuallyLiveBroadcast -> AppRoute.LiveBroadcast
                        restored && integrationState.profile?.onboardingCompleted == false &&
                            integrationState.onboarding?.creatorSetupCompleted == true -> AppRoute.PermissionHub
                        restored && integrationState.profile?.onboardingCompleted == false -> AppRoute.CreatorSetup
                        restored -> AppRoute.Main(AppDestination.Home)
                        else -> AppRoute.Welcome
                    }
                )
            }
            SplashScreen()
        }
        AppRoute.Welcome -> WelcomeScreen(onRouteChanged)
        AppRoute.SignIn -> ConnectedSignInScreen(integrationState, onRouteChanged)
        AppRoute.CreateAccount -> ConnectedCreateAccountScreen(integrationState, onRouteChanged)
        AppRoute.ForgotPassword -> ConnectedForgotPasswordScreen(integrationState, onRouteChanged)
        is AppRoute.VerifyEmail -> ConnectedVerifyEmailScreen(integrationState, route.flow, onRouteChanged)
        is AppRoute.CodeExpired -> CodeExpiredScreen(route.flow, onRouteChanged)
        AppRoute.CreateNewPassword -> ConnectedCreateNewPasswordScreen(integrationState, onRouteChanged)
        AppRoute.PasswordResetSuccess -> PasswordResetSuccessScreen(onRouteChanged)
        AppRoute.AccountCreatedSuccess -> AccountCreatedSuccessScreen(onRouteChanged)
        AppRoute.CreatorSetup -> CreatorSetupScreen(integrationState, onRouteChanged)
        AppRoute.CreatorContentType -> CreatorContentTypeScreen(integrationState, onRouteChanged)
        AppRoute.CreatorPlatforms -> CreatorPlatformsScreen(integrationState, onRouteChanged)
        AppRoute.CreatorExperience -> CreatorExperienceScreen(integrationState, onRouteChanged)
        AppRoute.CreatorGoal -> CreatorGoalScreen(integrationState, onRouteChanged)
        AppRoute.CreatorSetupComplete -> CreatorSetupCompleteScreen(integrationState, onRouteChanged)
        AppRoute.PermissionHub -> PermissionHubScreen(integrationState, permissionSetupController, onRouteChanged)
        AppRoute.NotificationPermission -> NotificationPermissionScreen(integrationState, permissionSetupController, onRouteChanged)
        AppRoute.MicrophonePermission -> MicrophonePermissionScreen(integrationState, permissionSetupController, onRouteChanged)
        AppRoute.CameraPermission -> CameraPermissionScreen(integrationState, permissionSetupController, onRouteChanged)
        AppRoute.ScreenCaptureEducation -> ScreenCaptureEducationScreen(integrationState, permissionSetupController, onRouteChanged)
        AppRoute.PermissionsComplete -> PermissionsCompleteScreen(integrationState, permissionSetupController, onRouteChanged)
        AppRoute.MicCameraPermission -> MicCameraPermissionScreen(onRouteChanged)
        AppRoute.StudioReady -> StudioReadyScreen(onRouteChanged)

        AppRoute.StreamReadiness -> Phase09StreamReadinessScreen(
            state = integrationState,
            streamState = streamState,
            permissions = permissionSetupController,
            sceneState = sceneState,
            onBack = home,
            onGoLive = { goMain(AppDestination.GoLive) },
        )
        AppRoute.QuickGoLive -> QuickGoLiveScreen(home)
        AppRoute.Notifications -> Phase26NotificationsScreen(
            state = integrationState,
            onRoute = onRouteChanged,
            onDestination = goMain,
        )
        AppRoute.NotificationDetail -> Phase26NotificationDetailScreen(
            state = integrationState,
            onRoute = onRouteChanged,
        )

        AppRoute.Connections -> Phase10ConnectionsScreen(integrationState, onRouteChanged, home)
        AppRoute.AddConnection -> Phase11AddDestinationScreen(integrationState, onRouteChanged, connections)
        AppRoute.PlatformAuthorization -> PlatformAuthorizationScreen(onRouteChanged) { onRouteChanged(AppRoute.AddConnection) }
        AppRoute.ChannelPicker -> ChannelPickerScreen { onRouteChanged(AppRoute.PlatformAuthorization) }
        AppRoute.CustomRtmp -> Phase11PlatformConnectScreen(
            state = integrationState,
            onSaved = { onRouteChanged(AppRoute.ConnectionDetail) },
            onBack = { onRouteChanged(AppRoute.AddConnection) },
        )

        AppRoute.ConnectionDetail -> Phase12ConnectionDetailScreen(integrationState, onRouteChanged, connections)
        AppRoute.EditConnection -> Phase12EditConnectionScreen(integrationState) { onRouteChanged(AppRoute.ConnectionDetail) }
        AppRoute.ConnectionTest -> ConnectionTestScreen { onRouteChanged(AppRoute.ConnectionDetail) }
        AppRoute.DisconnectConfirmation -> DisconnectConfirmationScreen { onRouteChanged(AppRoute.ConnectionDetail) }
        AppRoute.ConnectionTroubleshooting -> ConnectionTroubleshootingScreen { onRouteChanged(AppRoute.ConnectionDetail) }

        AppRoute.SceneLibrary -> Phase18ScenesScreen(
            sceneState = sceneState,
            state = integrationState,
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

        AppRoute.SceneEditor -> Phase19SceneEditorScreen(
            sceneState = sceneState,
            overlayState = overlayState,
            state = integrationState,
            onBack = { onRouteChanged(AppRoute.SceneLibrary) },
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

        AppRoute.FacecamEditor -> Phase22FacecamScreen(
            facecamState = facecamState,
            state = integrationState,
            onBack = { onRouteChanged(AppRoute.Main(AppDestination.Scenes)) },
        )
        AppRoute.ImageLogoEditor -> ImageLogoEditorScreen { onRouteChanged(AppRoute.AddSource) }
        AppRoute.TextEditor -> TextEditorScreen { onRouteChanged(AppRoute.AddSource) }
        AppRoute.BrowserSourceEditor -> BrowserSourceEditorScreen { onRouteChanged(AppRoute.AddSource) }
        AppRoute.BackgroundEditor -> BackgroundEditorScreen { onRouteChanged(AppRoute.AddSource) }

        AppRoute.ChatOverlayEditor -> ChatOverlayEditorScreen { onRouteChanged(AppRoute.AddSource) }
        AppRoute.AlertEditor -> AlertEditorScreen { onRouteChanged(AppRoute.AddSource) }
        AppRoute.GoalOverlayEditor -> GoalOverlayEditorScreen { onRouteChanged(AppRoute.AddSource) }
        AppRoute.AudioMixer -> Phase21AudioMixerScreen(
            streamState = streamState,
            state = integrationState,
            onBack = { onRouteChanged(AppRoute.Main(AppDestination.Scenes)) },
        )
        AppRoute.AudioAdvanced -> AudioAdvancedScreen { onRouteChanged(AppRoute.AudioMixer) }
        AppRoute.QualityCenter -> Phase23StreamQualityScreen(
            streamState = streamState,
            captureController = captureController,
            state = integrationState,
            onGoLive = { onRouteChanged(AppRoute.StreamDetails) },
            onBack = { onRouteChanged(AppRoute.Main(AppDestination.Scenes)) },
        )

        AppRoute.StreamDetails -> Phase13GoLiveOverviewScreen(
            state = integrationState,
            streamState = streamState,
            sceneState = sceneState,
            facecamState = facecamState,
            onRoute = onRouteChanged,
            onDestination = goMain,
        )
        AppRoute.DestinationSelection -> Phase13DestinationSelectionScreen(
            state = integrationState,
            onRoute = onRouteChanged,
            onBack = { onRouteChanged(AppRoute.StreamDetails) },
        )
        AppRoute.StreamInfo -> Phase13StreamInfoScreen(
            state = integrationState,
            onNext = { onRouteChanged(AppRoute.StreamQuality) },
            onBack = { onRouteChanged(AppRoute.DestinationSelection) },
        )
        AppRoute.StreamQuality -> Phase13StreamSettingsScreen(
            state = integrationState,
            streamState = streamState,
            onNext = { onRouteChanged(AppRoute.AudioCameraSetup) },
            onBack = { onRouteChanged(AppRoute.StreamInfo) },
        )
        AppRoute.AudioCameraSetup -> Phase13AudioCameraScreen(
            streamState = streamState,
            captureController = captureController,
            facecamState = facecamState,
            permissions = permissionSetupController,
            onRoute = onRouteChanged,
            onNext = { onRouteChanged(AppRoute.SetupReview) },
            onBack = { onRouteChanged(AppRoute.StreamQuality) },
        )
        AppRoute.SetupReview -> Phase13SetupReviewScreen(
            state = integrationState,
            streamState = streamState,
            captureController = captureController,
            facecamState = facecamState,
            permissions = permissionSetupController,
            onStartPreflight = { onRouteChanged(AppRoute.Preflight) },
            onBack = { onRouteChanged(AppRoute.AudioCameraSetup) },
        )

        AppRoute.Preflight -> Phase14PreflightStartScreen(
            onNext = { onRouteChanged(AppRoute.PreflightNetwork) },
            onBack = { onRouteChanged(AppRoute.SetupReview) },
        )
        AppRoute.PreflightNetwork -> Phase14NetworkCheckScreen(
            state = integrationState,
            streamState = streamState,
            onNext = { onRouteChanged(AppRoute.PreflightDevices) },
            onBack = { onRouteChanged(AppRoute.Preflight) },
        )
        AppRoute.PreflightDevices -> Phase14DeviceCheckScreen(
            state = integrationState,
            streamState = streamState,
            captureController = captureController,
            facecamState = facecamState,
            permissions = permissionSetupController,
            onRoute = onRouteChanged,
            onNext = { onRouteChanged(AppRoute.PreflightDestinations) },
            onBack = { onRouteChanged(AppRoute.PreflightNetwork) },
        )
        AppRoute.PreflightDestinations -> Phase14DestinationCheckScreen(
            state = integrationState,
            onNext = { onRouteChanged(AppRoute.PreflightSuccess) },
            onBack = { onRouteChanged(AppRoute.PreflightDevices) },
        )
        AppRoute.PreflightSuccess -> Phase14PreflightSuccessScreen(
            state = integrationState,
            streamState = streamState,
            captureController = captureController,
            permissions = permissionSetupController,
            onGoLive = { onRouteChanged(AppRoute.Countdown) },
            onBack = { onRouteChanged(AppRoute.PreflightDestinations) },
        )
        AppRoute.Countdown -> Phase14GoingLiveScreen(
            state = integrationState,
            streamState = streamState,
            captureController = captureController,
            facecamState = facecamState,
            overlayState = overlayState,
            sceneState = sceneState,
            onLive = { onRouteChanged(AppRoute.LiveBroadcast) },
            onBack = { onRouteChanged(AppRoute.PreflightSuccess) },
        )

        AppRoute.LiveBroadcast -> Phase15ActiveLiveScreen(
            state = integrationState,
            streamState = streamState,
            captureController = captureController,
            facecamState = facecamState,
            sceneState = sceneState,
            onRoute = onRouteChanged,
            onDestination = goMain,
        )
        AppRoute.LiveControls -> Phase15LiveControlsScreen(
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
        AppRoute.LiveChat -> Phase15LiveChatScreen { onRouteChanged(AppRoute.LiveBroadcast) }
        AppRoute.LiveStats -> Phase15LiveStatsScreen(
            streamState = streamState,
            captureController = captureController,
            onBack = { onRouteChanged(AppRoute.LiveBroadcast) },
        )
        AppRoute.LiveHealth -> Phase15StreamHealthScreen(
            streamState = streamState,
            captureController = captureController,
            onBack = { onRouteChanged(AppRoute.LiveBroadcast) },
        )
        AppRoute.LiveDestinations -> Phase15LiveDestinationsScreen(
            state = integrationState,
            captureController = captureController,
            onManage = { onRouteChanged(AppRoute.Connections) },
            onBack = { onRouteChanged(AppRoute.LiveBroadcast) },
        )
        AppRoute.EndStreamConfirmation -> Phase16EndStreamConfirmScreen(
            captureController = captureController,
            onEnd = { onRouteChanged(AppRoute.EndingStream) },
            onCancel = { onRouteChanged(AppRoute.LiveBroadcast) },
        )
        AppRoute.EndingStream -> Phase16EndingScreen(
            state = integrationState,
            captureController = captureController,
            onEnded = { onRouteChanged(AppRoute.StreamProcessing) },
        )
        AppRoute.LiveRecovery -> Phase16RecoveryHubScreen(
            captureController = captureController,
            onReconnect = { onRouteChanged(AppRoute.Reconnecting) },
            onEnd = { onRouteChanged(AppRoute.EndStreamConfirmation) },
            onReturn = { onRouteChanged(AppRoute.LiveBroadcast) },
        )
        AppRoute.NetworkDegraded -> Phase16RecoveryHubScreen(
            captureController = captureController,
            onReconnect = { onRouteChanged(AppRoute.Reconnecting) },
            onEnd = { onRouteChanged(AppRoute.EndStreamConfirmation) },
            onReturn = { onRouteChanged(AppRoute.LiveBroadcast) },
        )
        AppRoute.Reconnecting -> Phase16ReconnectingScreen(
            state = integrationState,
            streamState = streamState,
            captureController = captureController,
            facecamState = facecamState,
            overlayState = overlayState,
            sceneState = sceneState,
            onRecovered = { onRouteChanged(AppRoute.StreamRecovered) },
            onFailed = { onRouteChanged(AppRoute.RecoveryFailed) },
            onCancel = { onRouteChanged(AppRoute.LiveRecovery) },
        )
        AppRoute.StreamRecovered -> Phase16RecoveredScreen(
            captureController = captureController,
            onReturn = { onRouteChanged(AppRoute.LiveBroadcast) },
            onStats = { onRouteChanged(AppRoute.LiveStats) },
        )
        AppRoute.RecoveryFailed -> Phase16RecoveryFailedScreen(
            captureController = captureController,
            onRetry = { onRouteChanged(AppRoute.Reconnecting) },
            onEnd = { onRouteChanged(AppRoute.EndStreamConfirmation) },
        )
        AppRoute.DestinationFailure -> Phase16RecoveryHubScreen(
            captureController = captureController,
            onReconnect = { onRouteChanged(AppRoute.Reconnecting) },
            onEnd = { onRouteChanged(AppRoute.EndStreamConfirmation) },
            onReturn = { onRouteChanged(AppRoute.LiveBroadcast) },
        )
        AppRoute.StreamInterrupted -> Phase16RecoveryHubScreen(
            captureController = captureController,
            onReconnect = { onRouteChanged(AppRoute.Reconnecting) },
            onEnd = { onRouteChanged(AppRoute.EndStreamConfirmation) },
            onReturn = { onRouteChanged(AppRoute.LiveBroadcast) },
        )

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
        AppRoute.ActivityDetail -> Phase25StreamAnalyticsScreen(
            state = integrationState,
            captureController = captureController,
            onBack = { onRouteChanged(AppRoute.Main(AppDestination.Activity)) },
        )

        AppRoute.Plans -> Phase29MembershipScreen(
            state = integrationState,
            onRoute = onRouteChanged,
        )
        // Legacy purchase routes intentionally point back into the locked membership/billing flow.
        AppRoute.PlanComparison -> Phase29MembershipScreen(integrationState, onRouteChanged)
        AppRoute.CheckoutConfirmation -> Phase30BillingScreen(integrationState, onRouteChanged)
        AppRoute.PurchaseState -> Phase30BillingScreen(integrationState, onRouteChanged)
        AppRoute.ManageSubscription -> Phase30BillingScreen(integrationState, onRouteChanged)

        AppRoute.EditProfile -> Phase28EditProfileScreen(integrationState, onRouteChanged)
        AppRoute.Billing -> Phase30BillingScreen(integrationState, onRouteChanged)
        AppRoute.PaymentMethods -> Phase30PaymentMethodsScreen(onRouteChanged)
        AppRoute.SettingsHub -> Phase31SettingsScreen(
            state = integrationState,
            onRoute = onRouteChanged,
            onDestination = goMain,
        )
        AppRoute.StreamingSettings -> Phase32StreamingSettingsScreen(streamState, integrationState, onRouteChanged)
        AppRoute.AccountSettings -> Phase33AccountSettingsScreen(integrationState, onRouteChanged)

        // Legacy aliases retained for old deep-links.
        AppRoute.AccountSecurity -> Phase33AccountSettingsScreen(integrationState, onRouteChanged)
        AppRoute.StreamingDefaults -> Phase32StreamingSettingsScreen(streamState, integrationState, onRouteChanged)
        AppRoute.VideoAudioDefaults -> Phase32StreamingSettingsScreen(streamState, integrationState, onRouteChanged)
        AppRoute.AppearanceNotifications -> Phase31SettingsScreen(integrationState, onRouteChanged, goMain)

        AppRoute.HelpCenter -> Phase34SupportHubScreen(
            state = integrationState,
            onRoute = onRouteChanged,
            onBack = { onRouteChanged(AppRoute.SettingsHub) },
        )
        AppRoute.Troubleshooting -> Phase34TroubleshootingScreen(
            onRoute = onRouteChanged,
            onBack = { onRouteChanged(AppRoute.HelpCenter) },
        )
        AppRoute.ContactSupport -> Phase34ContactSupportScreen(
            state = integrationState,
            onBack = { onRouteChanged(AppRoute.HelpCenter) },
        )
        AppRoute.Diagnostics -> Phase35DiagnosticsScreen(
            state = integrationState,
            streamState = streamState,
            captureController = captureController,
            permissions = permissionSetupController,
            onRoute = onRouteChanged,
            onBack = { onRouteChanged(AppRoute.SettingsHub) },
        )
        AppRoute.LegalPrivacy -> Phase36LegalAboutScreen(
            state = integrationState,
            onBack = { onRouteChanged(AppRoute.SettingsHub) },
        )
        AppRoute.DeleteAccount -> DeleteAccountScreen(
            state = integrationState,
            onBack = { onRouteChanged(AppRoute.AccountSecurity) },
            onDeleted = { onRouteChanged(AppRoute.SignIn) },
        )

        AppRoute.Offline -> Phase37OfflineScreen(
            onRetry = { onRouteChanged(AppRoute.Main(AppDestination.Home)) },
            onHome = { onRouteChanged(AppRoute.Main(AppDestination.Home)) },
        )
        AppRoute.SessionExpired -> Phase37SessionExpiredScreen { onRouteChanged(AppRoute.SignIn) }
        AppRoute.PermissionBlocked -> Phase37PermissionBlockedScreen { onRouteChanged(AppRoute.SettingsHub) }
        AppRoute.ServiceError -> Phase37ServiceErrorScreen(
            onRetry = { onRouteChanged(AppRoute.Main(AppDestination.Home)) },
            onHome = { onRouteChanged(AppRoute.Main(AppDestination.Home)) },
        )
        AppRoute.DesignSystemStates -> Phase38GlobalComponentsScreen(
            state = integrationState,
            onRoute = onRouteChanged,
            onBack = { onRouteChanged(AppRoute.Diagnostics) },
        )
        AppRoute.GlobalComponents -> Phase38GlobalComponentsScreen(
            state = integrationState,
            onRoute = onRouteChanged,
            onBack = { onRouteChanged(AppRoute.Diagnostics) },
        )
        AppRoute.FunctionalQa -> Phase39FunctionalQaScreen(
            state = integrationState,
            streamState = streamState,
            captureController = captureController,
            permissions = permissionSetupController,
            onRoute = onRouteChanged,
            onDestination = goMain,
            onBack = { onRouteChanged(AppRoute.Diagnostics) },
        )

        is AppRoute.Main -> when (route.destination) {
            AppDestination.Home -> ConnectedHomeScreen(
                state = integrationState,
                streamState = streamState,
                captureController = captureController,
                sceneState = sceneState,
                permissions = permissionSetupController,
                onRoute = onRouteChanged,
                onDestination = goMain,
            )
            AppDestination.Scenes -> Phase17StudioScreen(
                sceneState = sceneState,
                overlayState = overlayState,
                facecamState = facecamState,
                onRoute = onRouteChanged,
                onDestination = goMain,
            )
            AppDestination.GoLive -> Phase13GoLiveOverviewScreen(
                state = integrationState,
                streamState = streamState,
                sceneState = sceneState,
                facecamState = facecamState,
                onRoute = onRouteChanged,
                onDestination = goMain,
            )
            AppDestination.Activity -> Phase24StreamHistoryScreen(
                state = integrationState,
                onOpenDetails = { id ->
                    integrationState.selectHistory(id)
                    onRouteChanged(AppRoute.ActivityDetail)
                },
                onDestination = goMain,
            )
            AppDestination.Settings -> Phase27ProfileScreen(
                state = integrationState,
                onRoute = onRouteChanged,
                onDestination = goMain,
            )
            AppDestination.Overlays -> Phase20OverlaysScreen(
                overlayState = overlayState,
                sceneState = sceneState,
                state = integrationState,
                onBack = { onRouteChanged(AppRoute.Main(AppDestination.Scenes)) },
            )
            AppDestination.Connections -> Phase10ConnectionsScreen(integrationState, onRouteChanged, home)
        }
    }
}
