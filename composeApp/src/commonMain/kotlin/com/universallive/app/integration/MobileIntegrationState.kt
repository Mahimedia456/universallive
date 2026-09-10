package com.universallive.app.integration

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MobileIntegrationState(
    val api: MobileBackendApi,
) {
    var session: MobileSession? by mutableStateOf(null)
        private set

    var profile: CreatorProfile? by mutableStateOf(null)
        private set

    var membership: Membership? by mutableStateOf(null)
        private set

    var onboarding: OnboardingState? by mutableStateOf(null)
        private set

    var pendingEmail: String by mutableStateOf("")
    var loading: Boolean by mutableStateOf(false)
        private set
    var accountLoading: Boolean by mutableStateOf(false)
        private set
    var error: String? by mutableStateOf(null)
        private set

    val isAuthenticated: Boolean
        get() = session != null

    fun clearError() {
        error = null
    }

    private fun messageOf(throwable: Throwable): String {
        val raw = throwable.message?.trim().orEmpty()
        return if (raw.isNotBlank()) raw else "Something went wrong. Please try again."
    }

    suspend fun restore(): Boolean {
        loading = true
        error = null
        return try {
            val restored = api.restoreSession().getOrThrow()
            session = restored
            if (restored != null) refreshAccount()
            restored != null
        } catch (_: Throwable) {
            session = null
            profile = null
            membership = null
            onboarding = null
            false
        } finally {
            loading = false
        }
    }

    suspend fun signIn(email: String, password: String): Boolean {
        loading = true
        error = null
        pendingEmail = email.trim()
        return try {
            session = api.signIn(email.trim(), password).getOrThrow()
            refreshAccount()
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            loading = false
        }
    }

    suspend fun signUp(
        fullName: String,
        username: String,
        email: String,
        password: String,
    ): Boolean {
        loading = true
        error = null
        pendingEmail = email.trim()
        return try {
            val createdSession = api.signUp(
                email = email.trim(),
                password = password,
                fullName = fullName.trim(),
                username = username.trim().removePrefix("@"),
            ).getOrThrow()

            session = createdSession
            if (createdSession != null) refreshAccount()
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            loading = false
        }
    }

    suspend fun verifyAccount(code: String): Boolean {
        loading = true
        error = null
        return try {
            val verifiedSession = api.verifyEmail(pendingEmail, code).getOrThrow()
            session = verifiedSession
            if (verifiedSession != null) refreshAccount()
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            loading = false
        }
    }

    suspend fun resendVerification(): Boolean {
        loading = true
        error = null
        return try {
            api.resendVerification(pendingEmail).getOrThrow()
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            loading = false
        }
    }

    suspend fun requestPasswordReset(email: String): Boolean {
        loading = true
        error = null
        pendingEmail = email.trim()
        return try {
            api.forgotPassword(email.trim()).getOrThrow()
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            loading = false
        }
    }

    suspend fun verifyRecovery(code: String): Boolean {
        loading = true
        error = null
        return try {
            session = api.verifyRecovery(pendingEmail, code).getOrThrow()
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            loading = false
        }
    }

    suspend fun updatePassword(password: String): Boolean {
        loading = true
        error = null
        return try {
            api.updatePassword(password).getOrThrow()
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            loading = false
        }
    }

    suspend fun refreshAccount() {
        if (session == null) return
        accountLoading = true
        try {
            profile = api.profile().getOrThrow()
            membership = api.membership().getOrThrow()
            onboarding = api.onboarding().getOrThrow()
        } catch (t: Throwable) {
            error = messageOf(t)
        } finally {
            accountLoading = false
        }
    }

    suspend fun saveProfile(displayName: String, username: String): Boolean {
        accountLoading = true
        error = null
        return try {
            profile = api.updateProfile(displayName, username).getOrThrow()
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            accountLoading = false
        }
    }

    suspend fun signOut() {
        loading = true
        runCatching { api.signOut().getOrThrow() }
        session = null
        profile = null
        membership = null
        onboarding = null
        pendingEmail = ""
        loading = false
        error = null
    }
}
