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

    var connections: List<StreamingConnection> by mutableStateOf(emptyList())
        private set

    var scenes: List<CloudScene> by mutableStateOf(emptyList())
        private set

    var selectedConnection: StreamingConnection? by mutableStateOf(null)
    var selectedScene: CloudScene? by mutableStateOf(null)

    suspend fun refreshConnections() {
        accountLoading = true
        error = null
        try {
            connections = api.connections().getOrThrow()
        } catch (t: Throwable) {
            error = messageOf(t)
        } finally {
            accountLoading = false
        }
    }

    suspend fun addConnection(platform: String, name: String, isDefault: Boolean = false): Boolean {
        accountLoading = true
        error = null
        return try {
            val item = api.createConnection(platform, name, isDefault).getOrThrow()
            selectedConnection = item
            refreshConnections()
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            accountLoading = false
        }
    }

    suspend fun saveCustomRtmp(name: String, serverUrl: String, streamKey: String): Boolean {
        accountLoading = true
        error = null
        return try {
            val connection = api.createConnection("custom_rtmp", name, connections.isEmpty()).getOrThrow()
            api.saveRtmpCredential(connection.id, serverUrl, streamKey).getOrThrow()
            selectedConnection = connection
            refreshConnections()
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            accountLoading = false
        }
    }

    suspend fun testSelectedConnection(): ConnectionTestResult? {
        val item = selectedConnection ?: return null
        accountLoading = true
        error = null
        return try {
            api.testConnection(item.id).getOrThrow()
        } catch (t: Throwable) {
            error = messageOf(t)
            null
        } finally {
            accountLoading = false
        }
    }

    suspend fun removeSelectedConnection(): Boolean {
        val item = selectedConnection ?: return false
        accountLoading = true
        error = null
        return try {
            api.deleteConnection(item.id).getOrThrow()
            selectedConnection = null
            refreshConnections()
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            accountLoading = false
        }
    }

    suspend fun refreshScenes() {
        accountLoading = true
        error = null
        try {
            scenes = api.scenes().getOrThrow()
            if (selectedScene == null) {
                selectedScene = scenes.firstOrNull { it.isDefault } ?: scenes.firstOrNull()
            }
        } catch (t: Throwable) {
            error = messageOf(t)
        } finally {
            accountLoading = false
        }
    }

    suspend fun addCloudScene(name: String): Boolean {
        accountLoading = true
        error = null
        return try {
            val scene = api.createScene(name.ifBlank { "New Scene" }, isDefault = scenes.isEmpty()).getOrThrow()
            selectedScene = scene
            refreshScenes()
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            accountLoading = false
        }
    }

    suspend fun duplicateSelectedScene(): Boolean {
        val scene = selectedScene ?: return false
        accountLoading = true
        error = null
        return try {
            selectedScene = api.duplicateScene(scene.id).getOrThrow()
            refreshScenes()
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            accountLoading = false
        }
    }

    suspend fun removeSelectedScene(): Boolean {
        val scene = selectedScene ?: return false
        accountLoading = true
        error = null
        return try {
            api.deleteScene(scene.id).getOrThrow()
            selectedScene = null
            refreshScenes()
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            accountLoading = false
        }
    }

    var activeBroadcast: BroadcastSession? by mutableStateOf(null)
        private set

    var streamHistory: List<StreamHistoryItem> by mutableStateOf(emptyList())
        private set

    var notifications: List<AppNotification> by mutableStateOf(emptyList())
        private set

    var supportTickets: List<SupportTicket> by mutableStateOf(emptyList())
        private set

    suspend fun beginBroadcast(
        title: String,
        connectionIds: List<String>,
        sceneId: String?,
    ): Boolean {
        loading = true
        error = null
        return try {
            val created = api.createBroadcastSession(title, connectionIds, sceneId).getOrThrow()
            activeBroadcast = api.startBroadcastSession(created.id).getOrThrow()
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            loading = false
        }
    }

    suspend fun heartbeat(
        bitrateKbps: Int? = null,
        fps: Double? = null,
        droppedFrames: Int? = null,
        networkStatus: String? = null,
    ) {
        val session = activeBroadcast ?: return
        runCatching {
            api.heartbeatBroadcastSession(session.id).getOrThrow()
            api.sendTelemetry(
                sessionId = session.id,
                bitrateKbps = bitrateKbps,
                fps = fps,
                droppedFrames = droppedFrames,
                networkStatus = networkStatus,
            ).getOrThrow()
        }.onFailure {
            error = messageOf(it)
        }
    }

    suspend fun endBroadcast(): Boolean {
        val session = activeBroadcast ?: return true
        loading = true
        error = null
        return try {
            activeBroadcast = api.endBroadcastSession(session.id).getOrThrow()
            refreshHistory()
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            loading = false
        }
    }

    suspend fun refreshHistory() {
        accountLoading = true
        error = null
        try {
            streamHistory = api.streamHistory().getOrThrow()
        } catch (t: Throwable) {
            error = messageOf(t)
        } finally {
            accountLoading = false
        }
    }

    suspend fun refreshNotifications() {
        accountLoading = true
        error = null
        try {
            notifications = api.notifications().getOrThrow()
        } catch (t: Throwable) {
            error = messageOf(t)
        } finally {
            accountLoading = false
        }
    }

    suspend fun markNotificationRead(id: String) {
        runCatching {
            api.markNotificationRead(id).getOrThrow()
            refreshNotifications()
        }.onFailure {
            error = messageOf(it)
        }
    }

    suspend fun refreshSupportTickets() {
        accountLoading = true
        error = null
        try {
            supportTickets = api.supportTickets().getOrThrow()
        } catch (t: Throwable) {
            error = messageOf(t)
        } finally {
            accountLoading = false
        }
    }

    suspend fun submitSupportTicket(
        category: String,
        subject: String,
        description: String,
    ): Boolean {
        accountLoading = true
        error = null
        return try {
            api.createSupportTicket(category, subject, description).getOrThrow()
            refreshSupportTickets()
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            accountLoading = false
        }
    }
}
