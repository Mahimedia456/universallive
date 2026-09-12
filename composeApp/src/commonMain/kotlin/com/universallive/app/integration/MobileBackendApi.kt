package com.universallive.app.integration

data class MobileSession(
    val accessToken: String,
    val refreshToken: String? = null,
    val userId: String? = null,
    val email: String? = null,
)

data class CreatorProfile(
    val userId: String = "",
    val displayName: String = "",
    val username: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val creatorType: String? = null,
    val onboardingCompleted: Boolean = false,
)

data class Membership(
    val planKey: String = "free",
    val planName: String = "Free",
    val status: String = "active",
    val maxSimultaneousDestinations: Int = 1,
    val maxResolution: String = "720p",
    val advancedScenes: Boolean = false,
    val advancedOverlays: Boolean = false,
    val advancedAnalytics: Boolean = false,
) {
    val badge: String
        get() = planName.uppercase()
}

data class OnboardingState(
    val creatorSetupCompleted: Boolean = false,
    val permissionEducationCompleted: Boolean = false,
    val firstDestinationPromptCompleted: Boolean = false,
    val microphoneAcknowledged: Boolean = false,
    val cameraAcknowledged: Boolean = false,
    val screenCaptureAcknowledged: Boolean = false,
    val notificationsAcknowledged: Boolean = false,
    val creatorContentTypes: Set<String> = emptySet(),
    val preferredPlatforms: Set<String> = emptySet(),
    val experienceLevel: String? = null,
    val primaryGoal: String? = null,
)

data class HomeDashboard(
    val readyToStream: Boolean = false,
    val destinationCount: Int = 0,
    val readyDestinationCount: Int = 0,
    val defaultDestinationId: String? = null,
    val defaultSceneName: String? = null,
    val resolution: String = "1080p",
    val fps: Int = 30,
    val bitrateKbps: Int = 6800,
    val activeBroadcastId: String? = null,
    val activeBroadcastStatus: String? = null,
)


data class StreamingConnection(
    val id: String,
    val platform: String,
    val displayName: String,
    val status: String = "disconnected",
    val isDefault: Boolean = false,
    val isEnabled: Boolean = true,
    val credentialConfigured: Boolean = false,
    val readyToPublish: Boolean = false,
    val credentialUpdatedAt: String? = null,
    val lastTestedAt: String? = null,
    val lastErrorMessage: String? = null,
)

data class PublishConfig(
    val connectionId: String,
    val platform: String,
    val displayName: String,
    val serverUrl: String,
    val streamKey: String,
)

data class ConnectionTestResult(
    val ok: Boolean,
    val connectionId: String,
    val status: String,
)

data class CloudScene(
    val id: String,
    val name: String,
    val description: String? = null,
    val aspectRatio: String = "16:9",
    val width: Int = 1920,
    val height: Int = 1080,
    val isDefault: Boolean = false,
    val thumbnailUrl: String? = null,
    val templateKey: String? = null,
)

data class CloudSceneSource(
    val id: String,
    val sceneId: String,
    val sourceType: String,
    val name: String,
    val zIndex: Int = 0,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val x: Double = 0.0,
    val y: Double = 0.0,
    val width: Double = 1.0,
    val height: Double = 1.0,
    val rotation: Double = 0.0,
    val opacity: Double = 1.0,
)


data class BroadcastSession(
    val id: String,
    val title: String? = null,
    val status: String = "created",
    val startedAt: String? = null,
    val endedAt: String? = null,
)


data class StreamPreflightResult(
    val id: String,
    val ready: Boolean,
    val status: String,
    val expiresAt: String? = null,
    val warnings: List<String> = emptyList(),
)

data class DeviceStreamTelemetry(
    val connectionId: String? = null,
    val bitrateKbps: Int? = null,
    val targetBitrateKbps: Int? = null,
    val encoderBitrateKbps: Int? = null,
    val rtmpUploadKbps: Int? = null,
    val fps: Double? = null,
    val encodedFps: Double? = null,
    val sentFps: Double? = null,
    val droppedFrames: Int? = null,
    val publishedVideoFrames: Long? = null,
    val publishedAudioFrames: Long? = null,
    val encoderWidth: Int? = null,
    val encoderHeight: Int? = null,
    val encoderName: String? = null,
    val networkStatus: String? = null,
    val publishStatus: String? = null,
    val audioStatus: String? = null,
    val rtmpQueueDepth: Int? = null,
    val socketWriteLatencyMs: Int? = null,
    val publisherEnqueueLatencyMs: Int? = null,
    val lastVideoPacketAgeMs: Int? = null,
    val lastAudioPacketAgeMs: Int? = null,
    val keyframeIntervalMs: Int? = null,
    val videoPtsMonotonic: Boolean? = null,
    val audioPtsMonotonic: Boolean? = null,
    val reconnectCount: Int? = null,
    val publisherInstanceId: String? = null,
)

data class StreamRecoveryResult(
    val session: BroadcastSession,
    val publishConfig: PublishConfig?,
    val publishConfigs: List<PublishConfig> = emptyList(),
    val recoveryAttempt: Int = 0,
    val recoverableUntil: String? = null,
)

data class StreamDiagnostics(
    val health: String = "starting",
    val issues: List<String> = emptyList(),
)

data class StreamHistoryItem(
    val id: String,
    val title: String? = null,
    val status: String = "",
    val startedAt: String? = null,
    val endedAt: String? = null,
    val durationSeconds: Int? = null,
    val avgBitrateKbps: Int? = null,
    val avgFps: Double? = null,
    val droppedFrames: Int = 0,
)


data class StudioAudioConfig(
    val microphoneEnabled: Boolean = true,
    val internalAudioEnabled: Boolean = true,
    val microphoneGain: Double = 1.0,
    val internalAudioGain: Double = 1.0,
    val monitoringEnabled: Boolean = false,
    val preset: String = "Streaming",
)

data class StudioFacecamConfig(
    val enabled: Boolean = false,
    val lens: String = "front",
    val shape: String = "rounded",
    val size: Double = 0.25,
    val x: Double = 0.72,
    val y: Double = 0.05,
    val mirror: Boolean = true,
    val background: String = "None",
)

data class StudioWorkspaceSnapshot(
    val activeSceneId: String? = null,
    val qualityConfigId: String? = null,
    val audio: StudioAudioConfig = StudioAudioConfig(),
    val facecam: StudioFacecamConfig = StudioFacecamConfig(),
    val adaptiveBitrateEnabled: Boolean = true,
    val autosaveEnabled: Boolean = true,
)

data class StudioQualityConfig(
    val id: String? = null,
    val resolution: String = "1080p",
    val width: Int = 1920,
    val height: Int = 1080,
    val fps: Int = 30,
    val bitrateKbps: Int = 6800,
    val orientation: String = "auto",
    val keyframeIntervalSeconds: Int = 2,
    val audioBitrateKbps: Int = 160,
    val audioSampleRateHz: Int = 48000,
    val adaptiveBitrateEnabled: Boolean = true,
)

data class StreamAnalyticsDetail(
    val sessionId: String,
    val healthGrade: String = "unknown",
    val durationSeconds: Int? = null,
    val sampleCount: Int = 0,
    val avgBitrateKbps: Int? = null,
    val minBitrateKbps: Int? = null,
    val peakBitrateKbps: Int? = null,
    val avgRtmpUploadKbps: Int? = null,
    val avgFps: Double? = null,
    val avgEncodedFps: Double? = null,
    val avgSentFps: Double? = null,
    val droppedFrames: Int = 0,
    val reconnectCount: Int = 0,
    val warningCount: Int = 0,
    val errorCount: Int = 0,
)

data class AppNotification(
    val id: String,
    val title: String,
    val body: String,
    val isRead: Boolean = false,
    val createdAt: String? = null,
    val actionRoute: String? = null,
)

data class UserSettings(
    val notificationsEnabled: Boolean = true,
    val marketingNotificationsEnabled: Boolean = false,
    val streamDefaultsJson: String = "{}",
)

data class AccountSessionInfo(
    val id: String,
    val userAgent: String? = null,
    val active: Boolean = false,
    val createdAt: String? = null,
    val lastUsedAt: String? = null,
    val expiresAt: String? = null,
)

data class BillingPlanInfo(
    val planKey: String,
    val name: String,
    val description: String? = null,
    val monthlyProductId: String? = null,
    val yearlyProductId: String? = null,
    val displayMonthly: String? = null,
    val displayYearly: String? = null,
)

data class SupportTicket(
    val id: String,
    val category: String,
    val subject: String,
    val description: String,
    val status: String,
    val priority: String,
    val createdAt: String? = null,
)

data class DiagnosticSummary(
    val ok: Boolean = false,
    val generatedAt: String? = null,
    val firebaseConfigured: Boolean = false,
    val smtpConfigured: Boolean = false,
    val connectionCount: Int = 0,
    val sceneCount: Int = 0,
    val activeSessionCount: Int = 0,
    val deviceCount: Int = 0,
    val unreadNotificationCount: Int = 0,
    val mobileContractVersion: String = "2026.09-final",
)

data class LegalDocumentInfo(
    val documentKey: String,
    val title: String,
    val version: String,
    val body: String = "",
    val publicUrl: String? = null,
    val effectiveAt: String? = null,
    val requiredAcceptance: Boolean = false,
)

data class LegalAboutInfo(
    val appName: String = "Universal Live",
    val appVersion: String = "0.40.0",
    val buildNumber: Int = 39,
    val apiVersion: String = "v1",
    val mobileContractVersion: String = "2026.09-final",
    val documents: List<LegalDocumentInfo> = emptyList(),
)

data class SystemStateSnapshot(
    val online: Boolean = true,
    val maintenanceEnabled: Boolean = false,
    val maintenanceMessage: String? = null,
    val minimumAndroidVersion: String? = null,
    val forceUpdate: Boolean = false,
    val streamingEnabled: Boolean = true,
    val studioEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val billingEnabled: Boolean = true,
    val supportEnabled: Boolean = true,
)

data class FinalQaCheck(
    val key: String,
    val pass: Boolean,
    val required: Boolean,
)

data class FinalQaRun(
    val id: String? = null,
    val status: String = "review",
    val requiredPassed: Boolean = false,
    val checks: List<FinalQaCheck> = emptyList(),
    val generatedAt: String? = null,
)

interface MobileBackendApi {
    val baseUrl: String

    suspend fun restoreSession(): Result<MobileSession?>
    suspend fun signIn(email: String, password: String): Result<MobileSession>
    suspend fun signUp(
        email: String,
        password: String,
        fullName: String,
        username: String,
    ): Result<MobileSession?>

    suspend fun verifyEmail(email: String, token: String): Result<MobileSession?>
    suspend fun resendVerification(email: String): Result<Unit>
    suspend fun forgotPassword(email: String): Result<Unit>
    suspend fun verifyRecovery(email: String, token: String): Result<MobileSession>
    suspend fun updatePassword(password: String): Result<Unit>
    suspend fun signOut(): Result<Unit>

    suspend fun profile(): Result<CreatorProfile>
    suspend fun updateProfile(
        displayName: String,
        username: String,
        onboardingCompleted: Boolean? = null,
        creatorType: String? = null,
    ): Result<CreatorProfile>

    suspend fun membership(): Result<Membership>
    suspend fun billingPlans(): Result<List<BillingPlanInfo>>
    suspend fun registerDevice(): Result<Unit>
    suspend fun homeDashboard(): Result<HomeDashboard>
    suspend fun onboarding(): Result<OnboardingState>
    suspend fun updateOnboarding(state: OnboardingState): Result<OnboardingState>

    suspend fun connections(): Result<List<StreamingConnection>>
    suspend fun createConnection(
        platform: String,
        displayName: String,
        isDefault: Boolean = false,
    ): Result<StreamingConnection>
    suspend fun updateConnection(
        id: String,
        displayName: String? = null,
        isEnabled: Boolean? = null,
        isDefault: Boolean? = null,
    ): Result<StreamingConnection>
    suspend fun deleteConnection(id: String): Result<Unit>
    suspend fun testConnection(id: String): Result<ConnectionTestResult>
    suspend fun saveRtmpCredential(
        connectionId: String,
        serverUrl: String,
        streamKey: String,
    ): Result<Unit>
    suspend fun publishConfig(connectionId: String): Result<PublishConfig>

    suspend fun scenes(): Result<List<CloudScene>>
    suspend fun createScene(
        name: String,
        description: String? = null,
        isDefault: Boolean = false,
    ): Result<CloudScene>
    suspend fun updateScene(
        id: String,
        name: String? = null,
        isDefault: Boolean? = null,
    ): Result<CloudScene>
    suspend fun duplicateScene(id: String): Result<CloudScene>
    suspend fun deleteScene(id: String): Result<Unit>
    suspend fun sceneSources(sceneId: String): Result<List<CloudSceneSource>>
    suspend fun studioWorkspace(): Result<StudioWorkspaceSnapshot>
    suspend fun activateScene(id: String): Result<Unit>
    suspend fun saveStudioAudio(config: StudioAudioConfig): Result<StudioAudioConfig>
    suspend fun saveStudioFacecam(config: StudioFacecamConfig): Result<StudioFacecamConfig>
    suspend fun studioQuality(): Result<StudioQualityConfig?>
    suspend fun saveStudioQuality(config: StudioQualityConfig): Result<StudioQualityConfig>
    suspend fun upsertSceneSource(
        sceneId: String,
        sourceKey: String,
        sourceType: String,
        name: String,
        configJson: String = "{}",
    ): Result<CloudSceneSource>
    suspend fun updateSceneSource(
        sourceId: String,
        isVisible: Boolean? = null,
        zIndex: Int? = null,
        configJson: String? = null,
    ): Result<CloudSceneSource>
    suspend fun deleteSceneSource(sourceId: String): Result<Unit>

    suspend fun saveStreamDraft(
        title: String,
        description: String,
        category: String,
        privacy: String,
        connectionId: String,
        sceneId: String?,
        width: Int,
        height: Int,
        fps: Int,
        bitrateKbps: Int,
        microphoneEnabled: Boolean,
        internalAudioEnabled: Boolean,
        orientation: String,
    ): Result<Unit>
    suspend fun runStreamPreflight(
        title: String,
        connectionIds: List<String>,
        sceneId: String?,
        width: Int,
        height: Int,
        fps: Int,
        bitrateKbps: Int,
        microphoneEnabled: Boolean,
        internalAudioEnabled: Boolean,
        orientation: String,
    ): Result<StreamPreflightResult>
    suspend fun createBroadcastSession(
        title: String,
        connectionIds: List<String>,
        sceneId: String? = null,
        preflightId: String? = null,
    ): Result<BroadcastSession>
    suspend fun startBroadcastSession(id: String): Result<BroadcastSession>
    suspend fun activeBroadcastSession(): Result<BroadcastSession?>
    suspend fun heartbeatBroadcastSession(id: String): Result<Unit>
    suspend fun reportPublisherState(sessionId: String, telemetry: DeviceStreamTelemetry): Result<BroadcastSession>
    suspend fun recoverBroadcastSession(id: String, publisherInstanceId: String?): Result<StreamRecoveryResult>
    suspend fun streamDiagnostics(id: String): Result<StreamDiagnostics>
    suspend fun endBroadcastSession(id: String): Result<BroadcastSession>
    suspend fun sendTelemetry(sessionId: String, telemetry: DeviceStreamTelemetry): Result<Unit>
    suspend fun streamHistory(): Result<List<StreamHistoryItem>>
    suspend fun streamHistoryFiltered(status: String? = null, limit: Int = 25, offset: Int = 0): Result<List<StreamHistoryItem>>
    suspend fun streamAnalytics(sessionId: String): Result<StreamAnalyticsDetail>
    suspend fun notifications(): Result<List<AppNotification>>
    suspend fun notificationUnreadCount(): Result<Int>
    suspend fun markNotificationRead(id: String): Result<Unit>
    suspend fun markAllNotificationsRead(): Result<Unit>
    suspend fun sendTestPush(): Result<String>
    suspend fun settings(): Result<UserSettings>
    suspend fun saveStreamingSettings(streamDefaultsJson: String): Result<UserSettings>
    suspend fun accountSessions(): Result<List<AccountSessionInfo>>
    suspend fun logoutAllSessions(): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
    suspend fun supportTickets(): Result<List<SupportTicket>>
    suspend fun createSupportTicket(
        category: String,
        subject: String,
        description: String,
    ): Result<SupportTicket>
    suspend fun diagnosticsSummary(): Result<DiagnosticSummary>
    suspend fun createDiagnosticSnapshot(
        appVersion: String,
        buildNumber: String,
        captureStatus: String,
        publishStatus: String,
    ): Result<String>
    suspend fun legalAbout(): Result<LegalAboutInfo>
    suspend fun legalDocuments(): Result<List<LegalDocumentInfo>>
    suspend fun acceptLegal(documentKey: String, version: String): Result<Unit>
    suspend fun systemState(): Result<SystemStateSnapshot>
    suspend fun runFinalQa(): Result<FinalQaRun>
}

class OfflineMobileBackendApi : MobileBackendApi {
    override val baseUrl: String = ""

    private fun <T> unavailable(): Result<T> =
        Result.failure(IllegalStateException("Backend API is unavailable on this platform build."))

    override suspend fun restoreSession() = Result.success<MobileSession?>(null)
    override suspend fun signIn(email: String, password: String) = unavailable<MobileSession>()
    override suspend fun signUp(email: String, password: String, fullName: String, username: String) = unavailable<MobileSession?>()
    override suspend fun verifyEmail(email: String, token: String) = unavailable<MobileSession?>()
    override suspend fun resendVerification(email: String) = unavailable<Unit>()
    override suspend fun forgotPassword(email: String) = unavailable<Unit>()
    override suspend fun verifyRecovery(email: String, token: String) = unavailable<MobileSession>()
    override suspend fun updatePassword(password: String) = unavailable<Unit>()
    override suspend fun signOut() = Result.success(Unit)
    override suspend fun profile() = unavailable<CreatorProfile>()
    override suspend fun updateProfile(
        displayName: String,
        username: String,
        onboardingCompleted: Boolean?,
        creatorType: String?,
    ) = unavailable<CreatorProfile>()
    override suspend fun membership() = unavailable<Membership>()
    override suspend fun billingPlans() = unavailable<List<BillingPlanInfo>>()
    override suspend fun registerDevice() = Result.success(Unit)
    override suspend fun homeDashboard() = unavailable<HomeDashboard>()
    override suspend fun onboarding() = unavailable<OnboardingState>()
    override suspend fun updateOnboarding(state: OnboardingState) = unavailable<OnboardingState>()

    override suspend fun connections() = unavailable<List<StreamingConnection>>()
    override suspend fun createConnection(platform: String, displayName: String, isDefault: Boolean) = unavailable<StreamingConnection>()
    override suspend fun updateConnection(id: String, displayName: String?, isEnabled: Boolean?, isDefault: Boolean?) = unavailable<StreamingConnection>()
    override suspend fun deleteConnection(id: String) = unavailable<Unit>()
    override suspend fun testConnection(id: String) = unavailable<ConnectionTestResult>()
    override suspend fun saveRtmpCredential(connectionId: String, serverUrl: String, streamKey: String) = unavailable<Unit>()
    override suspend fun publishConfig(connectionId: String) = unavailable<PublishConfig>()
    override suspend fun scenes() = unavailable<List<CloudScene>>()
    override suspend fun createScene(name: String, description: String?, isDefault: Boolean) = unavailable<CloudScene>()
    override suspend fun updateScene(id: String, name: String?, isDefault: Boolean?) = unavailable<CloudScene>()
    override suspend fun duplicateScene(id: String) = unavailable<CloudScene>()
    override suspend fun deleteScene(id: String) = unavailable<Unit>()
    override suspend fun sceneSources(sceneId: String) = unavailable<List<CloudSceneSource>>()
    override suspend fun studioWorkspace() = unavailable<StudioWorkspaceSnapshot>()
    override suspend fun activateScene(id: String) = unavailable<Unit>()
    override suspend fun saveStudioAudio(config: StudioAudioConfig) = unavailable<StudioAudioConfig>()
    override suspend fun saveStudioFacecam(config: StudioFacecamConfig) = unavailable<StudioFacecamConfig>()
    override suspend fun studioQuality() = unavailable<StudioQualityConfig?>()
    override suspend fun saveStudioQuality(config: StudioQualityConfig) = unavailable<StudioQualityConfig>()
    override suspend fun upsertSceneSource(sceneId: String, sourceKey: String, sourceType: String, name: String, configJson: String) = unavailable<CloudSceneSource>()
    override suspend fun updateSceneSource(sourceId: String, isVisible: Boolean?, zIndex: Int?, configJson: String?) = unavailable<CloudSceneSource>()
    override suspend fun deleteSceneSource(sourceId: String) = unavailable<Unit>()

    override suspend fun saveStreamDraft(title: String, description: String, category: String, privacy: String, connectionId: String, sceneId: String?, width: Int, height: Int, fps: Int, bitrateKbps: Int, microphoneEnabled: Boolean, internalAudioEnabled: Boolean, orientation: String) = unavailable<Unit>()
    override suspend fun runStreamPreflight(title: String, connectionIds: List<String>, sceneId: String?, width: Int, height: Int, fps: Int, bitrateKbps: Int, microphoneEnabled: Boolean, internalAudioEnabled: Boolean, orientation: String) = unavailable<StreamPreflightResult>()
    override suspend fun createBroadcastSession(title: String, connectionIds: List<String>, sceneId: String?, preflightId: String?) = unavailable<BroadcastSession>()
    override suspend fun startBroadcastSession(id: String) = unavailable<BroadcastSession>()
    override suspend fun activeBroadcastSession() = unavailable<BroadcastSession?>()
    override suspend fun heartbeatBroadcastSession(id: String) = unavailable<Unit>()
    override suspend fun reportPublisherState(sessionId: String, telemetry: DeviceStreamTelemetry) = unavailable<BroadcastSession>()
    override suspend fun recoverBroadcastSession(id: String, publisherInstanceId: String?) = unavailable<StreamRecoveryResult>()
    override suspend fun streamDiagnostics(id: String) = unavailable<StreamDiagnostics>()
    override suspend fun endBroadcastSession(id: String) = unavailable<BroadcastSession>()
    override suspend fun sendTelemetry(sessionId: String, telemetry: DeviceStreamTelemetry) = unavailable<Unit>()
    override suspend fun streamHistory() = unavailable<List<StreamHistoryItem>>()
    override suspend fun streamHistoryFiltered(status: String?, limit: Int, offset: Int) = unavailable<List<StreamHistoryItem>>()
    override suspend fun streamAnalytics(sessionId: String) = unavailable<StreamAnalyticsDetail>()
    override suspend fun notifications() = unavailable<List<AppNotification>>()
    override suspend fun notificationUnreadCount() = unavailable<Int>()
    override suspend fun markNotificationRead(id: String) = unavailable<Unit>()
    override suspend fun markAllNotificationsRead() = unavailable<Unit>()
    override suspend fun sendTestPush() = unavailable<String>()
    override suspend fun settings() = unavailable<UserSettings>()
    override suspend fun saveStreamingSettings(streamDefaultsJson: String) = unavailable<UserSettings>()
    override suspend fun accountSessions() = unavailable<List<AccountSessionInfo>>()
    override suspend fun logoutAllSessions() = unavailable<Unit>()
    override suspend fun deleteAccount() = unavailable<Unit>()
    override suspend fun supportTickets() = unavailable<List<SupportTicket>>()
    override suspend fun createSupportTicket(category: String, subject: String, description: String) = unavailable<SupportTicket>()
    override suspend fun diagnosticsSummary() = unavailable<DiagnosticSummary>()
    override suspend fun createDiagnosticSnapshot(appVersion: String, buildNumber: String, captureStatus: String, publishStatus: String) = unavailable<String>()
    override suspend fun legalAbout() = unavailable<LegalAboutInfo>()
    override suspend fun legalDocuments() = unavailable<List<LegalDocumentInfo>>()
    override suspend fun acceptLegal(documentKey: String, version: String) = unavailable<Unit>()
    override suspend fun systemState() = unavailable<SystemStateSnapshot>()
    override suspend fun runFinalQa() = unavailable<FinalQaRun>()
}
