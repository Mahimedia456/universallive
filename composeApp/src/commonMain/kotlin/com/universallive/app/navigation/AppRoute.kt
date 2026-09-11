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

    // Phase 07 — Creator Onboarding
    data object CreatorSetup : AppRoute
    data object CreatorContentType : AppRoute
    data object CreatorPlatforms : AppRoute
    data object CreatorExperience : AppRoute
    data object CreatorGoal : AppRoute
    data object CreatorSetupComplete : AppRoute

    // Phase 08 - Permissions Setup
    data object PermissionHub : AppRoute
    data object NotificationPermission : AppRoute
    data object MicrophonePermission : AppRoute
    data object CameraPermission : AppRoute
    data object ScreenCaptureEducation : AppRoute
    data object PermissionsComplete : AppRoute

    // Legacy Phase 08 aliases kept temporarily so older deep-links/source references compile.
    data object MicCameraPermission : AppRoute
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

    // Locked roadmap Phase 23 — Stream Quality Center
    data object QualityCenter : AppRoute

    // Locked roadmap Phase 13 — Go Live Setup
    data object StreamDetails : AppRoute
    data object DestinationSelection : AppRoute
    data object StreamInfo : AppRoute
    data object StreamQuality : AppRoute
    data object AudioCameraSetup : AppRoute
    data object SetupReview : AppRoute

    // Locked roadmap Phase 14 — Stream Preflight
    data object Preflight : AppRoute
    data object PreflightNetwork : AppRoute
    data object PreflightDevices : AppRoute
    data object PreflightDestinations : AppRoute
    data object PreflightSuccess : AppRoute
    data object Countdown : AppRoute

    // Locked roadmap Phase 15 — Active Live
    data object LiveBroadcast : AppRoute
    data object LiveControls : AppRoute
    data object LiveSceneSwitcher : AppRoute
    data object LiveChat : AppRoute
    data object LiveStats : AppRoute
    data object LiveHealth : AppRoute
    data object LiveDestinations : AppRoute
    data object EndStreamConfirmation : AppRoute

    // Locked roadmap Phase 16 — Disconnect / Recovery
    data object EndingStream : AppRoute
    data object LiveRecovery : AppRoute
    data object NetworkDegraded : AppRoute
    data object Reconnecting : AppRoute
    data object StreamRecovered : AppRoute
    data object RecoveryFailed : AppRoute
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


    // Locked roadmap Phase 27-33 — Profile, Membership, Billing & Settings
    data object EditProfile : AppRoute
    data object Billing : AppRoute
    data object PaymentMethods : AppRoute
    data object SettingsHub : AppRoute
    data object StreamingSettings : AppRoute
    data object AccountSettings : AppRoute

    // Legacy aliases retained so existing deep-links/source references keep compiling.
    data object AccountSecurity : AppRoute
    data object StreamingDefaults : AppRoute
    data object VideoAudioDefaults : AppRoute
    data object AppearanceNotifications : AppRoute

    // Locked roadmap Phase 34-36 — Support, Diagnostics, Legal / About
    data object HelpCenter : AppRoute
    data object Troubleshooting : AppRoute
    data object ContactSupport : AppRoute
    data object Diagnostics : AppRoute
    data object LegalPrivacy : AppRoute
    data object DeleteAccount : AppRoute

    // Locked roadmap Phase 37-39 — Global states, components and functional QA
    data object GlobalComponents : AppRoute
    data object FunctionalQa : AppRoute

    // Legacy production-state routes retained for deep links and runtime failures.
    data object Offline : AppRoute
    data object SessionExpired : AppRoute
    data object PermissionBlocked : AppRoute
    data object ServiceError : AppRoute
    data object DesignSystemStates : AppRoute

    data class Main(val destination: AppDestination) : AppRoute
}
