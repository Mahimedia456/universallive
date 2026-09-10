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
}
