package com.universallive.app.navigation

enum class VerificationFlow {
    AccountCreation,
    PasswordRecovery,
}

sealed interface AppRoute {
    data object Splash : AppRoute
    data object Welcome : AppRoute
    data object SignIn : AppRoute
    data object CreateAccount : AppRoute
    data object ForgotPassword : AppRoute

    data class VerifyEmail(val flow: VerificationFlow) : AppRoute
    data class CodeExpired(val flow: VerificationFlow) : AppRoute
    data object CreateNewPassword : AppRoute
    data object PasswordResetSuccess : AppRoute
    data object AccountCreatedSuccess : AppRoute

    data object CreatorSetup : AppRoute
    data object PermissionHub : AppRoute
    data object MicCameraPermission : AppRoute
    data object ScreenCaptureEducation : AppRoute
    data object StudioReady : AppRoute

    // Phase 04 — Home + Notifications
    data object StreamReadiness : AppRoute
    data object QuickGoLive : AppRoute
    data object Notifications : AppRoute
    data object NotificationDetail : AppRoute

    // Phase 05 — Connections
    data object Connections : AppRoute
    data object AddConnection : AppRoute
    data object PlatformAuthorization : AppRoute
    data object ChannelPicker : AppRoute
    data object CustomRtmp : AppRoute

    // Phase 06 — Connection management
    data object ConnectionDetail : AppRoute
    data object EditConnection : AppRoute
    data object ConnectionTest : AppRoute
    data object DisconnectConfirmation : AppRoute
    data object ConnectionTroubleshooting : AppRoute


    // Phase 07 — Studio + Scene Library
    data object SceneLibrary : AppRoute
    data object CreateScene : AppRoute
    data object SceneTemplates : AppRoute
    data object SceneOptions : AppRoute

    // Phase 08 — Core Scene Editor
    data object SceneEditor : AppRoute
    data object AddSource : AppRoute
    data object Layers : AppRoute
    data object TransformInspector : AppRoute
    data object SourceProperties : AppRoute

    // Phase 09 — Visual Source Editors
    data object FacecamEditor : AppRoute
    data object ImageLogoEditor : AppRoute
    data object TextEditor : AppRoute
    data object BrowserSourceEditor : AppRoute
    data object BackgroundEditor : AppRoute


    // Phase 10 — Interactive Overlays + Audio
    data object ChatOverlayEditor : AppRoute
    data object AlertEditor : AppRoute
    data object GoalOverlayEditor : AppRoute
    data object AudioMixer : AppRoute
    data object AudioAdvanced : AppRoute

    // Phase 11 — Go Live Configuration
    data object StreamDetails : AppRoute
    data object DestinationSelection : AppRoute
    data object StreamQuality : AppRoute
    data object Preflight : AppRoute
    data object Countdown : AppRoute

    // Phase 12 — Active Broadcast
    data object LiveBroadcast : AppRoute
    data object LiveControls : AppRoute
    data object LiveSceneSwitcher : AppRoute
    data object LiveChat : AppRoute
    data object LiveStats : AppRoute
    data object EndStreamConfirmation : AppRoute


    // Phase 13 — Broadcast Resilience
    data object LiveRecovery : AppRoute
    data object NetworkDegraded : AppRoute
    data object Reconnecting : AppRoute
    data object DestinationFailure : AppRoute
    data object StreamInterrupted : AppRoute

    // Phase 14 — Post Stream + Activity
    data object StreamProcessing : AppRoute
    data object StreamSummary : AppRoute
    data object StreamPerformance : AppRoute
    data object ActivityDetail : AppRoute

    // Phase 15 — Subscription + Billing
    data object Plans : AppRoute
    data object PlanComparison : AppRoute
    data object CheckoutConfirmation : AppRoute
    data object PurchaseState : AppRoute
    data object ManageSubscription : AppRoute


    // Phase 16 — Profile + Settings
    data object AccountSecurity : AppRoute
    data object StreamingDefaults : AppRoute
    data object VideoAudioDefaults : AppRoute
    data object AppearanceNotifications : AppRoute

    // Phase 17 — Support + Account Lifecycle
    data object HelpCenter : AppRoute
    data object Troubleshooting : AppRoute
    data object ContactSupport : AppRoute
    data object LegalPrivacy : AppRoute
    data object DeleteAccount : AppRoute

    // Phase 18 — Production States + Final UI QA
    data object Offline : AppRoute
    data object SessionExpired : AppRoute
    data object PermissionBlocked : AppRoute
    data object ServiceError : AppRoute
    data object DesignSystemStates : AppRoute

    data class Main(val destination: AppDestination) : AppRoute
}
