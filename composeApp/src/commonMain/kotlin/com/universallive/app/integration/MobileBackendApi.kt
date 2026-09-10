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

data class AppNotification(
    val id: String,
    val title: String,
    val body: String,
    val isRead: Boolean = false,
    val createdAt: String? = null,
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
    ): Result<CreatorProfile>

    suspend fun membership(): Result<Membership>
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

    suspend fun createBroadcastSession(
        title: String,
        connectionIds: List<String>,
        sceneId: String? = null,
    ): Result<BroadcastSession>
    suspend fun startBroadcastSession(id: String): Result<BroadcastSession>
    suspend fun activeBroadcastSession(): Result<BroadcastSession?>
    suspend fun heartbeatBroadcastSession(id: String): Result<Unit>
    suspend fun endBroadcastSession(id: String): Result<BroadcastSession>
    suspend fun sendTelemetry(
        sessionId: String,
        bitrateKbps: Int?,
        fps: Double?,
        droppedFrames: Int?,
        networkStatus: String?,
    ): Result<Unit>
    suspend fun streamHistory(): Result<List<StreamHistoryItem>>
    suspend fun notifications(): Result<List<AppNotification>>
    suspend fun markNotificationRead(id: String): Result<Unit>
    suspend fun supportTickets(): Result<List<SupportTicket>>
    suspend fun createSupportTicket(
        category: String,
        subject: String,
        description: String,
    ): Result<SupportTicket>
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
    override suspend fun updateProfile(displayName: String, username: String, onboardingCompleted: Boolean?) = unavailable<CreatorProfile>()
    override suspend fun membership() = unavailable<Membership>()
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

    override suspend fun createBroadcastSession(title: String, connectionIds: List<String>, sceneId: String?) = unavailable<BroadcastSession>()
    override suspend fun startBroadcastSession(id: String) = unavailable<BroadcastSession>()
    override suspend fun activeBroadcastSession() = unavailable<BroadcastSession?>()
    override suspend fun heartbeatBroadcastSession(id: String) = unavailable<Unit>()
    override suspend fun endBroadcastSession(id: String) = unavailable<BroadcastSession>()
    override suspend fun sendTelemetry(sessionId: String, bitrateKbps: Int?, fps: Double?, droppedFrames: Int?, networkStatus: String?) = unavailable<Unit>()
    override suspend fun streamHistory() = unavailable<List<StreamHistoryItem>>()
    override suspend fun notifications() = unavailable<List<AppNotification>>()
    override suspend fun markNotificationRead(id: String) = unavailable<Unit>()
    override suspend fun supportTickets() = unavailable<List<SupportTicket>>()
    override suspend fun createSupportTicket(category: String, subject: String, description: String) = unavailable<SupportTicket>()
}
