package com.universallive.app.integration

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.universallive.app.streaming.facecam.FacecamLens
import com.universallive.app.streaming.facecam.FacecamShape
import com.universallive.app.streaming.facecam.FacecamState
import com.universallive.app.streaming.model.StreamFps
import com.universallive.app.streaming.model.StreamOrientation
import com.universallive.app.streaming.model.StreamResolution
import com.universallive.app.streaming.overlays.OverlayKind
import com.universallive.app.streaming.overlays.OverlayLayer
import com.universallive.app.streaming.overlays.OverlayState
import com.universallive.app.streaming.overlays.SceneState
import com.universallive.app.streaming.overlays.StreamScene
import com.universallive.app.streaming.state.StreamConfigState

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

    var homeDashboard: HomeDashboard? by mutableStateOf(null)
        private set

    var pendingEmail: String by mutableStateOf("")
    var loading: Boolean by mutableStateOf(false)
        private set
    var accountLoading: Boolean by mutableStateOf(false)
        private set
    var error: String? by mutableStateOf(null)
        private set

    // Phase 07 creator-onboarding draft. These choices are kept in the app state while
    // the user moves through the six onboarding screens. The primary creator type and
    // completion flags are persisted through the existing backend contract.
    var onboardingContentTypes: Set<String> by mutableStateOf(setOf("Gaming"))
        private set
    var onboardingPlatforms: Set<String> by mutableStateOf(setOf("youtube"))
        private set
    var onboardingExperience: String by mutableStateOf("new")
        private set
    var onboardingGoal: String by mutableStateOf("gaming")
        private set

    val isAuthenticated: Boolean
        get() = session != null

    val hasActuallyLiveBroadcast: Boolean
        get() = activeBroadcast?.status?.trim()?.lowercase() in setOf("live", "reconnecting")

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
            systemState = api.systemState().getOrNull()
            val restored = api.restoreSession().getOrThrow()
            session = restored
            if (restored != null) {
                refreshAccount()
                val restoredBroadcast = api.activeBroadcastSession().getOrNull()
                activeBroadcast = restoredBroadcast?.takeIf {
                    it.status.trim().lowercase() in setOf("live", "reconnecting")
                }
            }
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
            billingPlans = api.billingPlans().getOrDefault(emptyList())
            userSettings = api.settings().getOrNull()
            accountSessions = api.accountSessions().getOrDefault(emptyList())
            notificationUnreadCount = api.notificationUnreadCount().getOrDefault(notificationUnreadCount)
            onboarding = api.onboarding().getOrThrow().also { saved ->
                if (saved.creatorContentTypes.isNotEmpty()) onboardingContentTypes = saved.creatorContentTypes
                if (saved.preferredPlatforms.isNotEmpty()) onboardingPlatforms = saved.preferredPlatforms
                saved.experienceLevel?.takeIf { it.isNotBlank() }?.let { onboardingExperience = it }
                saved.primaryGoal?.takeIf { it.isNotBlank() }?.let { onboardingGoal = it }
            }
            runCatching { api.registerDevice().getOrThrow() }
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

    fun toggleOnboardingContentType(value: String) {
        onboardingContentTypes = if (value in onboardingContentTypes) {
            if (onboardingContentTypes.size == 1) onboardingContentTypes else onboardingContentTypes - value
        } else {
            onboardingContentTypes + value
        }
    }

    fun toggleOnboardingPlatform(value: String) {
        onboardingPlatforms = if (value in onboardingPlatforms) {
            if (onboardingPlatforms.size == 1) onboardingPlatforms else onboardingPlatforms - value
        } else {
            onboardingPlatforms + value
        }
    }

    fun chooseOnboardingExperience(value: String) {
        onboardingExperience = value
    }

    fun chooseOnboardingGoal(value: String) {
        onboardingGoal = value
    }

    suspend fun acknowledgePermission(
        notification: Boolean? = null,
        microphone: Boolean? = null,
        camera: Boolean? = null,
        screenCapture: Boolean? = null,
    ): Boolean {
        accountLoading = true
        error = null
        return try {
            val current = onboarding ?: OnboardingState(creatorSetupCompleted = true)
            val updated = current.copy(
                notificationsAcknowledged = notification ?: current.notificationsAcknowledged,
                microphoneAcknowledged = microphone ?: current.microphoneAcknowledged,
                cameraAcknowledged = camera ?: current.cameraAcknowledged,
                screenCaptureAcknowledged = screenCapture ?: current.screenCaptureAcknowledged,
            )
            onboarding = api.updateOnboarding(updated).getOrThrow()
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            accountLoading = false
        }
    }

    suspend fun completePermissionSetup(
        notificationsAcknowledged: Boolean = onboarding?.notificationsAcknowledged ?: false,
        microphoneAcknowledged: Boolean = onboarding?.microphoneAcknowledged ?: false,
        cameraAcknowledged: Boolean = onboarding?.cameraAcknowledged ?: false,
    ): Boolean {
        accountLoading = true
        error = null
        return try {
            val current = onboarding ?: OnboardingState(creatorSetupCompleted = true)
            val updated = current.copy(
                permissionEducationCompleted = true,
                notificationsAcknowledged = notificationsAcknowledged,
                microphoneAcknowledged = microphoneAcknowledged,
                cameraAcknowledged = cameraAcknowledged,
                screenCaptureAcknowledged = true,
            )
            onboarding = api.updateOnboarding(updated).getOrThrow()

            val currentProfile = profile
            if (currentProfile != null) {
                profile = api.updateProfile(
                    displayName = currentProfile.displayName,
                    username = currentProfile.username,
                    onboardingCompleted = true,
                    creatorType = currentProfile.creatorType,
                ).getOrThrow()
            }
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            accountLoading = false
        }
    }

    suspend fun completeCreatorSetup(): Boolean {
        accountLoading = true
        error = null
        return try {
            val current = onboarding ?: OnboardingState()
            val updated = current.copy(
                creatorSetupCompleted = true,
                creatorContentTypes = onboardingContentTypes,
                preferredPlatforms = onboardingPlatforms,
                experienceLevel = onboardingExperience,
                primaryGoal = onboardingGoal,
            )
            onboarding = api.updateOnboarding(updated).getOrThrow()

            val currentProfile = profile
            if (currentProfile != null) {
                val primaryType = onboardingContentTypes.firstOrNull()?.lowercase()?.replace(" / ", "_")
                    ?.replace(" ", "_")
                profile = api.updateProfile(
                    displayName = currentProfile.displayName,
                    username = currentProfile.username,
                    creatorType = primaryType,
                ).getOrThrow()
            }
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
        homeDashboard = null
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

    var pendingConnectionPlatform: String by mutableStateOf("custom_rtmp")
        private set

    var connectionsOpenedFromSettings: Boolean by mutableStateOf(false)
        private set

    fun beginConnectionsFlow(fromSettings: Boolean = false) {
        connectionsOpenedFromSettings = fromSettings
        error = null
    }

    fun finishConnectionsFlow() {
        connectionsOpenedFromSettings = false
    }

    var liveTitle: String by mutableStateOf("Tonight's Live Session")
    var liveDescription: String by mutableStateOf("")
    var livePrivacy: String by mutableStateOf("Public")
    var liveCategory: String by mutableStateOf("Gaming")
    var selectedLiveConnectionId: String? by mutableStateOf(null)
        private set
    var selectedLiveConnectionIds: Set<String> by mutableStateOf(emptySet())
        private set
    var preparedPublishConfig: PublishConfig? by mutableStateOf(null)
        private set
    var preparedPublishConfigs: List<PublishConfig> by mutableStateOf(emptyList())
        private set
    var streamPreflight: StreamPreflightResult? by mutableStateOf(null)
        private set
    var streamDiagnostics: StreamDiagnostics? by mutableStateOf(null)
        private set

    fun beginConnectionSetup(platform: String) {
        pendingConnectionPlatform = platform.trim().lowercase().ifBlank { "custom_rtmp" }
        error = null
    }

    fun platformLabel(platform: String = pendingConnectionPlatform): String = when (platform.lowercase()) {
        "youtube" -> "YouTube"
        "facebook" -> "Facebook Live"
        "twitch" -> "Twitch"
        "tiktok" -> "TikTok Live"
        else -> "Custom RTMP"
    }

    fun defaultServerUrl(platform: String = pendingConnectionPlatform): String = when (platform.lowercase()) {
        "youtube" -> "rtmps://a.rtmps.youtube.com/live2"
        "facebook" -> "rtmps://live-api-s.facebook.com:443/rtmp/"
        "twitch" -> "rtmp://live.twitch.tv/app"
        else -> ""
    }

    fun chooseLiveConnection(id: String) {
        val item = connections.firstOrNull { it.id == id } ?: return
        selectedLiveConnectionId = id
        selectedLiveConnectionIds = setOf(id)
        selectedConnection = item
        preparedPublishConfig = null
        preparedPublishConfigs = emptyList()
        streamPreflight = null
        error = null
    }

    fun toggleLiveConnection(id: String) {
        val item = connections.firstOrNull { it.id == id && it.readyToPublish && it.isEnabled } ?: return
        val max = (membership?.maxSimultaneousDestinations ?: 1).coerceAtLeast(1)
        val next = selectedLiveConnectionIds.toMutableSet()
        if (id in next) {
            if (next.size > 1) next.remove(id)
        } else {
            if (next.size >= max) {
                error = "Your current plan allows up to $max simultaneous destination(s)."
                return
            }
            next.add(id)
        }
        selectedLiveConnectionIds = next
        selectedLiveConnectionId = next.firstOrNull()
        selectedConnection = selectedLiveConnectionId?.let { primary -> connections.firstOrNull { it.id == primary } } ?: item
        preparedPublishConfig = null
        preparedPublishConfigs = emptyList()
        streamPreflight = null
        error = null
    }

    suspend fun refreshHomeDashboard() {
        if (session == null) return
        try {
            homeDashboard = api.homeDashboard().getOrThrow()
        } catch (_: Throwable) {
            // Home retains its existing local/API fallbacks if aggregation is temporarily unavailable.
        }
    }

    suspend fun refreshConnections() {
        accountLoading = true
        error = null
        try {
            connections = api.connections().getOrThrow()
            selectedConnection = selectedConnection?.let { selected ->
                connections.firstOrNull { it.id == selected.id }
            }

            val validIds = connections.filter { it.readyToPublish && it.isEnabled }.map { it.id }.toSet()
            selectedLiveConnectionIds = selectedLiveConnectionIds.intersect(validIds)
            if (selectedLiveConnectionIds.isEmpty()) {
                val fallback = connections
                    .firstOrNull { it.isDefault && it.readyToPublish && it.isEnabled }?.id
                    ?: connections.firstOrNull { it.readyToPublish && it.isEnabled }?.id
                selectedLiveConnectionIds = fallback?.let { setOf(it) } ?: emptySet()
            }
            selectedLiveConnectionId = selectedLiveConnectionIds.firstOrNull()
            preparedPublishConfig = null
            preparedPublishConfigs = emptyList()
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

    suspend fun saveRtmpConnection(
        platform: String,
        name: String,
        serverUrl: String,
        streamKey: String,
    ): Boolean {
        accountLoading = true
        error = null
        return try {
            val connection = api.createConnection(platform, name, connections.isEmpty()).getOrThrow()
            try {
                api.saveRtmpCredential(connection.id, serverUrl, streamKey).getOrThrow()
            } catch (credentialError: Throwable) {
                runCatching { api.deleteConnection(connection.id).getOrThrow() }
                throw credentialError
            }
            selectedConnection = connection
            selectedLiveConnectionId = connection.id
            refreshConnections()
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            accountLoading = false
        }
    }

    suspend fun saveCustomRtmp(name: String, serverUrl: String, streamKey: String): Boolean =
        saveRtmpConnection("custom_rtmp", name, serverUrl, streamKey)

    suspend fun updateSelectedConnection(
        displayName: String,
        isEnabled: Boolean,
        isDefault: Boolean,
        serverUrl: String? = null,
        streamKey: String? = null,
    ): Boolean {
        val item = selectedConnection ?: return false
        accountLoading = true
        error = null
        return try {
            api.updateConnection(
                id = item.id,
                displayName = displayName.trim(),
                isEnabled = isEnabled,
                isDefault = isDefault,
            ).getOrThrow()

            if (!serverUrl.isNullOrBlank() || !streamKey.isNullOrBlank()) {
                if (serverUrl.isNullOrBlank() || streamKey.isNullOrBlank()) {
                    throw IllegalArgumentException("Enter both server URL and stream key to replace credentials.")
                }
                api.saveRtmpCredential(item.id, serverUrl.trim(), streamKey.trim()).getOrThrow()
            }

            refreshConnections()
            selectedConnection = connections.firstOrNull { it.id == item.id }
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            accountLoading = false
        }
    }

    suspend fun prepareSelectedPublishConfig(): Boolean {
        val ids = selectedLiveConnectionIds.ifEmpty { selectedLiveConnectionId?.let { setOf(it) } ?: emptySet() }
        if (ids.isEmpty()) {
            error = "Choose at least one ready destination first."
            return false
        }
        loading = true
        error = null
        return try {
            val configs = ids.map { id -> api.publishConfig(id).getOrThrow() }
            preparedPublishConfigs = configs
            preparedPublishConfig = configs.firstOrNull()
            configs.isNotEmpty()
        } catch (t: Throwable) {
            preparedPublishConfig = null
            preparedPublishConfigs = emptyList()
            error = messageOf(t)
            false
        } finally {
            loading = false
        }
    }

    suspend fun runStreamPreflight(
        width: Int,
        height: Int,
        fps: Int,
        bitrateKbps: Int,
        microphoneEnabled: Boolean,
        internalAudioEnabled: Boolean,
        orientation: String,
    ): Boolean {
        val connectionIds = selectedLiveConnectionIds.ifEmpty { selectedLiveConnectionId?.let { setOf(it) } ?: emptySet() }.toList()
        val connectionId = connectionIds.firstOrNull() ?: run {
            error = "Choose at least one ready destination first."
            return false
        }
        loading = true
        error = null
        return try {
            // Validate the backend-owned route first. This produces precise preflight checks and
            // avoids turning a credential/deployment issue into a generic local publish error.
            api.saveStreamDraft(
                title = liveTitle,
                description = liveDescription,
                category = liveCategory,
                privacy = livePrivacy,
                connectionId = connectionId,
                sceneId = selectedScene?.id,
                width = width,
                height = height,
                fps = fps,
                bitrateKbps = bitrateKbps,
                microphoneEnabled = microphoneEnabled,
                internalAudioEnabled = internalAudioEnabled,
                orientation = orientation,
            ).getOrThrow()
            val result = api.runStreamPreflight(
                title = liveTitle,
                connectionIds = connectionIds,
                sceneId = selectedScene?.id,
                width = width,
                height = height,
                fps = fps,
                bitrateKbps = bitrateKbps,
                microphoneEnabled = microphoneEnabled,
                internalAudioEnabled = internalAudioEnabled,
                orientation = orientation,
            ).getOrThrow()
            streamPreflight = result
            if (!result.ready) {
                error = result.checks.firstOrNull { it.required && !it.ok }?.message
                    ?: result.warnings.firstOrNull()
                    ?: "Backend stream preflight did not pass."
                preparedPublishConfig = null
                preparedPublishConfigs = emptyList()
                false
            } else {
                // Secrets are requested only after the backend has verified ownership, entitlement,
                // credential decryption and RTMP/RTMPS route validity.
                val configs = connectionIds.map { id -> api.publishConfig(id).getOrThrow() }
                preparedPublishConfigs = configs
                preparedPublishConfig = configs.firstOrNull()
                configs.isNotEmpty()
            }
        } catch (t: Throwable) {
            streamPreflight = null
            error = messageOf(t)
            false
        } finally {
            loading = false
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
            if (selectedLiveConnectionId == item.id || item.id in selectedLiveConnectionIds) {
                selectedLiveConnectionIds = selectedLiveConnectionIds - item.id
                selectedLiveConnectionId = selectedLiveConnectionIds.firstOrNull()
                preparedPublishConfig = null
                preparedPublishConfigs = emptyList()
            }
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

    suspend fun hydrateStudio(
        sceneState: SceneState,
        overlayState: OverlayState,
        facecamState: FacecamState,
        streamState: StreamConfigState,
    ): Boolean {
        accountLoading = true
        error = null
        return try {
            scenes = api.scenes().getOrThrow()
            studioWorkspace = api.studioWorkspace().getOrThrow()
            studioQuality = api.studioQuality().getOrNull()

            if (scenes.isNotEmpty()) {
                val activeId = studioWorkspace?.activeSceneId
                    ?: scenes.firstOrNull { it.isDefault }?.id
                    ?: scenes.first().id
                selectedScene = scenes.firstOrNull { it.id == activeId } ?: scenes.first()
                sceneState.replaceFromBackend(
                    scenes.map { StreamScene(it.id, it.name, emptyList()) },
                    selectedScene?.id,
                )

                val sources = api.sceneSources(selectedScene!!.id).getOrElse { emptyList() }
                overlayState.replaceFromBackend(
                    sources.filter { it.sourceType in setOf("text", "image", "logo", "chat", "alert", "goal", "lower_third", "overlay") }
                        .map { source ->
                            OverlayLayer(
                                id = source.id,
                                kind = when (source.sourceType) {
                                    "image", "alert", "goal" -> OverlayKind.IMAGE
                                    "logo" -> OverlayKind.LOGO
                                    else -> OverlayKind.TEXT
                                },
                                label = source.name,
                                enabled = source.isVisible,
                                x = source.x.toFloat(),
                                y = source.y.toFloat(),
                                width = source.width.toFloat(),
                                opacity = source.opacity.toFloat(),
                            )
                        }
                )
            }

            studioWorkspace?.facecam?.let { saved ->
                facecamState.setEnabled(saved.enabled)
                facecamState.setLens(if (saved.lens.equals("back", true)) FacecamLens.BACK else FacecamLens.FRONT)
                facecamState.setShape(
                    when (saved.shape.lowercase()) {
                        "square" -> FacecamShape.SQUARE
                        "circle" -> FacecamShape.CIRCLE
                        else -> FacecamShape.ROUNDED
                    }
                )
                facecamState.setSize(saved.size.toFloat())
                facecamState.setPosition(saved.x.toFloat(), saved.y.toFloat())
                facecamState.setMirrored(saved.mirror)
            }

            studioWorkspace?.audio?.let { saved ->
                streamState.setMicrophoneEnabled(saved.microphoneEnabled)
                streamState.setInternalAudioEnabled(saved.internalAudioEnabled)
            }

            studioQuality?.let { saved ->
                val resolution = StreamResolution.entries.firstOrNull {
                    it.label.equals(saved.resolution, true) || it.label.contains(saved.height.toString())
                }
                if (resolution != null) streamState.setResolution(resolution)
                streamState.setFps(if (saved.fps >= 60) StreamFps.FPS60 else StreamFps.FPS30)
                streamState.setBitrateKbps(saved.bitrateKbps)
                StreamOrientation.entries.firstOrNull { it.label.equals(saved.orientation, true) }
                    ?.let(streamState::setOrientation)
            }
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            accountLoading = false
        }
    }

    suspend fun activateStudioScene(sceneId: String, sceneState: SceneState): Boolean {
        error = null
        return try {
            api.activateScene(sceneId).getOrThrow()
            selectedScene = scenes.firstOrNull { it.id == sceneId }
            sceneState.activate(sceneId)
            studioWorkspace = studioWorkspace?.copy(activeSceneId = sceneId)
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        }
    }

    suspend fun persistOverlayState(sceneId: String, overlayState: OverlayState): Boolean {
        error = null
        return try {
            overlayState.layers.forEachIndexed { index, layer ->
                val sourceType = when (layer.kind) {
                    OverlayKind.IMAGE -> "image"
                    OverlayKind.LOGO -> "logo"
                    OverlayKind.TEXT -> "text"
                }
                val config = buildString {
                    append("{\"text\":\"")
                    append(layer.text.replace("\\", "\\\\").replace("\"", "\\\""))
                    append("\",\"assetPath\":\"")
                    append(layer.assetPath.replace("\\", "\\\\").replace("\"", "\\\""))
                    append("\",\"x\":").append(layer.x)
                    append(",\"y\":").append(layer.y)
                    append(",\"width\":").append(layer.width)
                    append(",\"opacity\":").append(layer.opacity)
                    append("}")
                }
                api.upsertSceneSource(
                    sceneId = sceneId,
                    sourceKey = layer.id,
                    sourceType = sourceType,
                    name = layer.label,
                    configJson = config,
                ).getOrThrow()
            }
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        }
    }

    suspend fun persistFacecam(facecamState: FacecamState, background: String = "None"): Boolean {
        val c = facecamState.config
        error = null
        return try {
            val saved = api.saveStudioFacecam(
                StudioFacecamConfig(
                    enabled = c.enabled,
                    lens = c.lens.name.lowercase(),
                    shape = c.shape.name.lowercase(),
                    size = c.size.toDouble(),
                    x = c.x.toDouble(),
                    y = c.y.toDouble(),
                    mirror = c.mirrored,
                    background = background,
                )
            ).getOrThrow()
            studioWorkspace = studioWorkspace?.copy(facecam = saved)
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        }
    }

    suspend fun persistAudio(streamState: StreamConfigState, preset: String = "Streaming"): Boolean {
        val c = streamState.config
        error = null
        return try {
            val saved = api.saveStudioAudio(
                StudioAudioConfig(
                    microphoneEnabled = c.microphoneEnabled,
                    internalAudioEnabled = c.internalAudioEnabled,
                    preset = preset,
                )
            ).getOrThrow()
            studioWorkspace = studioWorkspace?.copy(audio = saved)
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        }
    }

    suspend fun persistQuality(streamState: StreamConfigState, adaptiveBitrateEnabled: Boolean): Boolean {
        val c = streamState.config
        error = null
        return try {
            studioQuality = api.saveStudioQuality(
                StudioQualityConfig(
                    id = studioQuality?.id,
                    resolution = c.resolution.label,
                    width = c.resolution.width,
                    height = c.resolution.height,
                    fps = c.fps.value,
                    bitrateKbps = c.bitrateKbps,
                    orientation = c.orientation.name.lowercase(),
                    adaptiveBitrateEnabled = adaptiveBitrateEnabled,
                )
            ).getOrThrow()
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
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

    var studioWorkspace: StudioWorkspaceSnapshot? by mutableStateOf(null)
        private set

    var studioQuality: StudioQualityConfig? by mutableStateOf(null)
        private set

    var selectedStreamAnalytics: StreamAnalyticsDetail? by mutableStateOf(null)
        private set

    var activeBroadcast: BroadcastSession? by mutableStateOf(null)
        private set

    var streamHistory: List<StreamHistoryItem> by mutableStateOf(emptyList())
        private set

    var selectedHistoryId: String? by mutableStateOf(null)
        private set

    val selectedHistoryItem: StreamHistoryItem?
        get() = selectedHistoryId?.let { id -> streamHistory.firstOrNull { it.id == id } }

    fun selectHistory(id: String) {
        if (streamHistory.any { it.id == id }) {
            selectedHistoryId = id
            selectedStreamAnalytics = null
        }
    }

    var notifications: List<AppNotification> by mutableStateOf(emptyList())
        private set

    var notificationUnreadCount: Int by mutableStateOf(0)
        private set

    var userSettings: UserSettings? by mutableStateOf(null)
        private set

    var accountSessions: List<AccountSessionInfo> by mutableStateOf(emptyList())
        private set

    var billingPlans: List<BillingPlanInfo> by mutableStateOf(emptyList())
        private set

    var selectedNotificationId: String? by mutableStateOf(null)
        private set

    val selectedNotificationItem: AppNotification?
        get() = selectedNotificationId?.let { id -> notifications.firstOrNull { it.id == id } }

    fun selectNotification(id: String) {
        if (notifications.any { it.id == id }) selectedNotificationId = id
    }

    var supportTickets: List<SupportTicket> by mutableStateOf(emptyList())
        private set

    var finalDiagnostics: DiagnosticSummary? by mutableStateOf(null)
        private set

    var diagnosticSnapshotId: String? by mutableStateOf(null)
        private set

    var legalAbout: LegalAboutInfo? by mutableStateOf(null)
        private set

    var legalDocuments: List<LegalDocumentInfo> by mutableStateOf(emptyList())
        private set

    var systemState: SystemStateSnapshot? by mutableStateOf(null)
        private set

    var finalQaRun: FinalQaRun? by mutableStateOf(null)
        private set

    suspend fun beginBroadcast(
        title: String,
        connectionIds: List<String>,
        sceneId: String?,
    ): Boolean {
        loading = true
        error = null
        return try {
            val preflight = streamPreflight
                ?: throw IllegalStateException("Run stream preflight again before going live.")
            if (!preflight.ready) throw IllegalStateException("Stream preflight has not passed.")
            val created = api.createBroadcastSession(
                title = title,
                connectionIds = connectionIds,
                sceneId = sceneId,
                preflightId = preflight.id,
            ).getOrThrow()
            activeBroadcast = api.startBroadcastSession(created.id).getOrThrow()
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            loading = false
        }
    }

    suspend fun heartbeat(telemetry: DeviceStreamTelemetry) {
        val session = activeBroadcast ?: return
        runCatching {
            api.heartbeatBroadcastSession(session.id).getOrThrow()
            api.sendTelemetry(session.id, telemetry).getOrThrow()
        }.onFailure {
            error = messageOf(it)
        }
    }

    suspend fun reportPublisherState(telemetry: DeviceStreamTelemetry) {
        val session = activeBroadcast ?: return
        runCatching {
            activeBroadcast = api.reportPublisherState(session.id, telemetry).getOrThrow()
        }.onFailure {
            error = messageOf(it)
        }
    }

    suspend fun recoverBroadcast(publisherInstanceId: String?): Boolean {
        val session = activeBroadcast ?: return false
        loading = true
        error = null
        return try {
            val recovery = api.recoverBroadcastSession(session.id, publisherInstanceId).getOrThrow()
            activeBroadcast = recovery.session
            val recoveredConfigs = recovery.publishConfigs.ifEmpty {
                recovery.publishConfig?.let { listOf(it) } ?: emptyList()
            }
            if (recoveredConfigs.isNotEmpty()) {
                preparedPublishConfigs = recoveredConfigs
                preparedPublishConfig = recoveredConfigs.first()
                selectedLiveConnectionIds = recoveredConfigs.map { it.connectionId }.filter { it.isNotBlank() }.toSet()
                selectedLiveConnectionId = selectedLiveConnectionIds.firstOrNull()
            }
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            loading = false
        }
    }

    suspend fun refreshStreamDiagnostics() {
        val session = activeBroadcast ?: return
        runCatching {
            streamDiagnostics = api.streamDiagnostics(session.id).getOrThrow()
        }
    }

    suspend fun endBroadcast(): Boolean {
        val session = activeBroadcast ?: return true
        loading = true
        error = null
        return try {
            activeBroadcast = api.endBroadcastSession(session.id).getOrThrow()
            streamPreflight = null
            preparedPublishConfig = null
            preparedPublishConfigs = emptyList()
            refreshHistory()
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            loading = false
        }
    }

    suspend fun refreshSelectedHistoryAnalytics() {
        val id = selectedHistoryId ?: return
        runCatching { api.streamAnalytics(id).getOrThrow() }
            .onSuccess { selectedStreamAnalytics = it }
            .onFailure { error = messageOf(it) }
    }

    suspend fun refreshHistory() {
        accountLoading = true
        error = null
        try {
            streamHistory = api.streamHistory().getOrThrow()
            if (selectedHistoryId == null || streamHistory.none { it.id == selectedHistoryId }) {
                selectedHistoryId = streamHistory.firstOrNull()?.id
            }
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
            notificationUnreadCount = notifications.count { !it.isRead }
            if (selectedNotificationId == null || notifications.none { it.id == selectedNotificationId }) {
                selectedNotificationId = notifications.firstOrNull()?.id
            }
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


    suspend fun markAllNotificationsRead() {
        runCatching {
            api.markAllNotificationsRead().getOrThrow()
            refreshNotifications()
        }.onFailure { error = messageOf(it) }
    }

    suspend fun sendTestPush(): Boolean {
        accountLoading = true
        error = null
        return try {
            api.sendTestPush().getOrThrow()
            refreshNotifications()
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            accountLoading = false
        }
    }

    suspend fun saveStreamingDefaults(streamDefaultsJson: String): Boolean {
        accountLoading = true
        error = null
        return try {
            userSettings = api.saveStreamingSettings(streamDefaultsJson).getOrThrow()
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            accountLoading = false
        }
    }

    suspend fun refreshAccountSessions() {
        accountSessions = api.accountSessions().getOrDefault(emptyList())
    }

    suspend fun logoutAllSessions(): Boolean {
        loading = true
        error = null
        return try {
            api.logoutAllSessions().getOrThrow()
            session = null
            profile = null
            membership = null
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            loading = false
        }
    }

    suspend fun deleteAccount(): Boolean {
        loading = true
        error = null
        return try {
            api.deleteAccount().getOrThrow()
            session = null
            profile = null
            membership = null
            notifications = emptyList()
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            loading = false
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


    suspend fun refreshSystemState() {
        runCatching { api.systemState().getOrThrow() }
            .onSuccess { systemState = it }
            .onFailure { error = messageOf(it) }
    }

    suspend fun refreshFinalDiagnostics(
        appVersion: String = "0.40.0",
        buildNumber: String = "39",
        captureStatus: String = "unknown",
        publishStatus: String = "unknown",
        persistSnapshot: Boolean = false,
    ): Boolean {
        accountLoading = true
        error = null
        return try {
            finalDiagnostics = api.diagnosticsSummary().getOrThrow()
            if (persistSnapshot) {
                diagnosticSnapshotId = api.createDiagnosticSnapshot(
                    appVersion = appVersion,
                    buildNumber = buildNumber,
                    captureStatus = captureStatus,
                    publishStatus = publishStatus,
                ).getOrThrow().takeIf { it.isNotBlank() }
            }
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            accountLoading = false
        }
    }

    suspend fun refreshLegal() {
        accountLoading = true
        error = null
        try {
            legalAbout = api.legalAbout().getOrThrow()
            legalDocuments = api.legalDocuments().getOrThrow()
        } catch (t: Throwable) {
            error = messageOf(t)
        } finally {
            accountLoading = false
        }
    }

    suspend fun acceptLegalDocument(documentKey: String, version: String): Boolean {
        accountLoading = true
        error = null
        return try {
            api.acceptLegal(documentKey, version).getOrThrow()
            true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            accountLoading = false
        }
    }

    suspend fun runFinalBackendQa(): Boolean {
        accountLoading = true
        error = null
        return try {
            finalQaRun = api.runFinalQa().getOrThrow()
            finalQaRun?.requiredPassed == true
        } catch (t: Throwable) {
            error = messageOf(t)
            false
        } finally {
            accountLoading = false
        }
    }
}
