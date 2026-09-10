package com.universallive.app.integration

import android.content.Context
import com.universallive.app.BuildConfig
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidMobileBackendApi(
    context: Context,
) : MobileBackendApi {

    override val baseUrl: String =
        BuildConfig.UNIVERSALLIVE_API_BASE_URL.trimEnd('/')

    private val prefs =
        context.getSharedPreferences("universallive_session", Context.MODE_PRIVATE)

    private var currentSession: MobileSession? = readSavedSession()

    private fun readSavedSession(): MobileSession? {
        val access = prefs.getString("access_token", null) ?: return null
        return MobileSession(
            accessToken = access,
            refreshToken = prefs.getString("refresh_token", null),
            userId = prefs.getString("user_id", null),
            email = prefs.getString("email", null),
        )
    }

    private fun saveSession(session: MobileSession?) {
        currentSession = session
        if (session == null) {
            prefs.edit().clear().apply()
            return
        }
        prefs.edit()
            .putString("access_token", session.accessToken)
            .putString("refresh_token", session.refreshToken)
            .putString("user_id", session.userId)
            .putString("email", session.email)
            .apply()
    }

    private fun jsonString(value: String?): String =
        JSONObject.quote(value ?: "")

    private fun errorMessage(code: Int, text: String): String {
        return runCatching {
            val json = JSONObject(text)
            when {
                json.has("message") -> {
                    val message = json.opt("message")
                    if (message is org.json.JSONArray) {
                        buildList {
                            for (i in 0 until message.length()) add(message.optString(i))
                        }.joinToString("\n")
                    } else {
                        json.optString("message")
                    }
                }
                json.has("error_description") -> json.optString("error_description")
                json.has("error") -> json.optString("error")
                else -> "API request failed ($code)"
            }
        }.getOrDefault("API request failed ($code)")
    }

    private suspend fun request(
        path: String,
        method: String = "GET",
        body: String? = null,
        authenticated: Boolean = false,
    ): String = withContext(Dispatchers.IO) {
        val url = URL("$baseUrl/${path.trimStart('/')}")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("X-UniversalLive-Client", "android")

            if (authenticated) {
                val token = currentSession?.accessToken
                    ?: throw IllegalStateException("Your session has expired. Please sign in again.")
                connection.setRequestProperty("Authorization", "Bearer $token")
            }

            if (body != null) {
                connection.doOutput = true
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use {
                    it.write(body)
                }
            }

            val status = connection.responseCode
            val input = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = if (input != null) {
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { it.readText() }
            } else ""

            if (status !in 200..299) {
                throw IllegalStateException(errorMessage(status, text))
            }
            text
        } finally {
            connection.disconnect()
        }
    }

    private fun sessionFrom(text: String): MobileSession? {
        if (text.isBlank()) return null
        val json = JSONObject(text)

        val access = json.optString("access_token").takeIf { it.isNotBlank() }
            ?: return null

        val user = json.optJSONObject("user")
        return MobileSession(
            accessToken = access,
            refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() },
            userId = user?.optString("id")?.takeIf { it.isNotBlank() },
            email = user?.optString("email")?.takeIf { it.isNotBlank() },
        )
    }

    override suspend fun restoreSession(): Result<MobileSession?> = runCatching {
        val saved = currentSession ?: return@runCatching null
        val refresh = saved.refreshToken

        if (refresh.isNullOrBlank()) {
            runCatching { profile().getOrThrow() }
                .onFailure { saveSession(null) }
            currentSession
        } else {
            val response = request(
                "auth/mobile/refresh",
                method = "POST",
                body = """{"refreshToken":${jsonString(refresh)}}""",
            )
            val refreshed = sessionFrom(response)
                ?: throw IllegalStateException("Session refresh did not return a valid session.")
            saveSession(refreshed.copy(email = refreshed.email ?: saved.email))
            currentSession
        }
    }

    override suspend fun signIn(email: String, password: String): Result<MobileSession> = runCatching {
        val response = request(
            "auth/mobile/login",
            method = "POST",
            body = """{"email":${jsonString(email)},"password":${jsonString(password)}}""",
        )
        val session = sessionFrom(response)
            ?: throw IllegalStateException("Login succeeded but no session was returned.")
        saveSession(session)
        session
    }

    override suspend fun signUp(
        email: String,
        password: String,
        fullName: String,
        username: String,
    ): Result<MobileSession?> = runCatching {
        val response = request(
            "auth/mobile/register",
            method = "POST",
            body = """
                {
                  "email":${jsonString(email)},
                  "password":${jsonString(password)},
                  "fullName":${jsonString(fullName)},
                  "username":${jsonString(username)}
                }
            """.trimIndent(),
        )
        val session = sessionFrom(response)
        if (session != null) saveSession(session)
        session
    }

    override suspend fun verifyEmail(email: String, token: String): Result<MobileSession?> = runCatching {
        val response = request(
            "auth/mobile/verify-email",
            method = "POST",
            body = """{"email":${jsonString(email)},"token":${jsonString(token)}}""",
        )
        val session = sessionFrom(response)
        if (session != null) saveSession(session)
        session
    }

    override suspend fun resendVerification(email: String): Result<Unit> = runCatching {
        request(
            "auth/mobile/resend-verification",
            method = "POST",
            body = """{"email":${jsonString(email)}}""",
        )
        Unit
    }

    override suspend fun forgotPassword(email: String): Result<Unit> = runCatching {
        request(
            "auth/mobile/forgot-password",
            method = "POST",
            body = """{"email":${jsonString(email)}}""",
        )
        Unit
    }

    override suspend fun verifyRecovery(email: String, token: String): Result<MobileSession> = runCatching {
        val response = request(
            "auth/mobile/verify-recovery",
            method = "POST",
            body = """{"email":${jsonString(email)},"token":${jsonString(token)}}""",
        )
        val session = sessionFrom(response)
            ?: throw IllegalStateException("Recovery verification did not return a session.")
        saveSession(session)
        session
    }

    override suspend fun updatePassword(password: String): Result<Unit> = runCatching {
        request(
            "auth/mobile/update-password",
            method = "POST",
            body = """{"password":${jsonString(password)}}""",
            authenticated = true,
        )
        Unit
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        runCatching {
            request(
                "auth/mobile/logout",
                method = "POST",
                body = "{}",
                authenticated = true,
            )
        }
        saveSession(null)
        Unit
    }

    override suspend fun profile(): Result<CreatorProfile> = runCatching {
        val response = request("profiles/me", authenticated = true)
        val json = JSONObject(response)
        CreatorProfile(
            userId = json.optString("user_id"),
            displayName = json.optString("display_name"),
            username = json.optString("username"),
            email = currentSession?.email.orEmpty(),
            avatarUrl = json.optString("avatar_url").takeIf { it.isNotBlank() && it != "null" },
            creatorType = json.optString("creator_type").takeIf { it.isNotBlank() && it != "null" },
            onboardingCompleted = json.optBoolean("onboarding_completed", false),
        )
    }

    override suspend fun updateProfile(
        displayName: String,
        username: String,
        onboardingCompleted: Boolean?,
    ): Result<CreatorProfile> = runCatching {
        val onboardingPart = onboardingCompleted?.let { ""","onboardingCompleted":$it""" } ?: ""
        val response = request(
            "profiles/me",
            method = "PUT",
            body = """
                {
                  "displayName":${jsonString(displayName)},
                  "username":${jsonString(username.removePrefix("@"))}
                  $onboardingPart
                }
            """.trimIndent(),
            authenticated = true,
        )
        val json = JSONObject(response)
        CreatorProfile(
            userId = json.optString("user_id"),
            displayName = json.optString("display_name"),
            username = json.optString("username"),
            email = currentSession?.email.orEmpty(),
            avatarUrl = json.optString("avatar_url").takeIf { it.isNotBlank() && it != "null" },
            creatorType = json.optString("creator_type").takeIf { it.isNotBlank() && it != "null" },
            onboardingCompleted = json.optBoolean("onboarding_completed", false),
        )
    }

    override suspend fun membership(): Result<Membership> = runCatching {
        val response = request("billing/entitlements/me", authenticated = true)
        val json = JSONObject(response)
        val plan = json.optJSONObject("plan")
        val effective = json.optJSONObject("effective_entitlements")
            ?: plan?.optJSONObject("entitlements")
            ?: JSONObject()

        val key = json.optString("plan_key", "free")
        Membership(
            planKey = key,
            planName = plan?.optString("name")?.takeIf { it.isNotBlank() }
                ?: key.replaceFirstChar { it.uppercase() },
            status = json.optString("status", "active"),
            maxSimultaneousDestinations =
                effective.optInt("max_simultaneous_destinations", 1),
            maxResolution = effective.optString("max_resolution", "720p"),
            advancedScenes = effective.optBoolean("advanced_scenes", false),
            advancedOverlays = effective.optBoolean("advanced_overlays", false),
            advancedAnalytics = effective.optBoolean("advanced_analytics", false),
        )
    }

    override suspend fun onboarding(): Result<OnboardingState> = runCatching {
        val response = request("onboarding/me", authenticated = true)
        val json = JSONObject(response)
        OnboardingState(
            creatorSetupCompleted = json.optBoolean("creator_setup_completed", false),
            permissionEducationCompleted = json.optBoolean("permission_education_completed", false),
            firstDestinationPromptCompleted = json.optBoolean("first_destination_prompt_completed", false),
            microphoneAcknowledged = json.optBoolean("microphone_acknowledged", false),
            cameraAcknowledged = json.optBoolean("camera_acknowledged", false),
            screenCaptureAcknowledged = json.optBoolean("screen_capture_acknowledged", false),
            notificationsAcknowledged = json.optBoolean("notifications_acknowledged", false),
        )
    }

    override suspend fun updateOnboarding(state: OnboardingState): Result<OnboardingState> = runCatching {
        request(
            "onboarding/me",
            method = "PUT",
            authenticated = true,
            body = """
                {
                  "creator_setup_completed":${state.creatorSetupCompleted},
                  "permission_education_completed":${state.permissionEducationCompleted},
                  "first_destination_prompt_completed":${state.firstDestinationPromptCompleted},
                  "microphone_acknowledged":${state.microphoneAcknowledged},
                  "camera_acknowledged":${state.cameraAcknowledged},
                  "screen_capture_acknowledged":${state.screenCaptureAcknowledged},
                  "notifications_acknowledged":${state.notificationsAcknowledged}
                }
            """.trimIndent(),
        )
        state
    }


    private fun connectionFrom(json: JSONObject) = StreamingConnection(
        id = json.optString("id"),
        platform = json.optString("platform"),
        displayName = json.optString("display_name"),
        status = json.optString("status", "disconnected"),
        isDefault = json.optBoolean("is_default", false),
        isEnabled = json.optBoolean("is_enabled", true),
        lastTestedAt = json.optString("last_tested_at").takeIf { it.isNotBlank() && it != "null" },
        lastErrorMessage = json.optString("last_error_message").takeIf { it.isNotBlank() && it != "null" },
    )

    override suspend fun connections(): Result<List<StreamingConnection>> = runCatching {
        val response = request("streaming/connections", authenticated = true)
        val array = org.json.JSONArray(response)
        buildList {
            for (i in 0 until array.length()) add(connectionFrom(array.getJSONObject(i)))
        }
    }

    override suspend fun createConnection(
        platform: String,
        displayName: String,
        isDefault: Boolean,
    ): Result<StreamingConnection> = runCatching {
        val response = request(
            "streaming/connections",
            method = "POST",
            authenticated = true,
            body = """{"platform":${jsonString(platform)},"displayName":${jsonString(displayName)},"isDefault":$isDefault}""",
        )
        connectionFrom(JSONObject(response))
    }

    override suspend fun updateConnection(
        id: String,
        displayName: String?,
        isEnabled: Boolean?,
        isDefault: Boolean?,
    ): Result<StreamingConnection> = runCatching {
        val fields = mutableListOf<String>()
        if (displayName != null) fields += """"displayName":${jsonString(displayName)}"""
        if (isEnabled != null) fields += """"isEnabled":$isEnabled"""
        if (isDefault != null) fields += """"isDefault":$isDefault"""
        val response = request(
            "streaming/connections/$id",
            method = "PATCH",
            authenticated = true,
            body = "{${fields.joinToString(",")}}",
        )
        connectionFrom(JSONObject(response))
    }

    override suspend fun deleteConnection(id: String): Result<Unit> = runCatching {
        request("streaming/connections/$id", method = "DELETE", authenticated = true)
        Unit
    }

    override suspend fun testConnection(id: String): Result<ConnectionTestResult> = runCatching {
        val response = request("streaming/connections/$id/test", method = "POST", body = "{}", authenticated = true)
        val json = JSONObject(response)
        ConnectionTestResult(
            ok = json.optBoolean("ok", false),
            connectionId = json.optString("connectionId", id),
            status = json.optString("status", "unknown"),
        )
    }

    override suspend fun saveRtmpCredential(
        connectionId: String,
        serverUrl: String,
        streamKey: String,
    ): Result<Unit> = runCatching {
        request(
            "streaming/rtmp",
            method = "POST",
            authenticated = true,
            body = """{"connectionId":${jsonString(connectionId)},"serverUrl":${jsonString(serverUrl)},"streamKey":${jsonString(streamKey)}}""",
        )
        Unit
    }

    private fun sceneFrom(json: JSONObject) = CloudScene(
        id = json.optString("id"),
        name = json.optString("name"),
        description = json.optString("description").takeIf { it.isNotBlank() && it != "null" },
        aspectRatio = json.optString("aspect_ratio", "16:9"),
        width = json.optInt("width", 1920),
        height = json.optInt("height", 1080),
        isDefault = json.optBoolean("is_default", false),
        thumbnailUrl = json.optString("thumbnail_url").takeIf { it.isNotBlank() && it != "null" },
        templateKey = json.optString("template_key").takeIf { it.isNotBlank() && it != "null" },
    )

    override suspend fun scenes(): Result<List<CloudScene>> = runCatching {
        val response = request("studio/scenes", authenticated = true)
        val array = org.json.JSONArray(response)
        buildList {
            for (i in 0 until array.length()) add(sceneFrom(array.getJSONObject(i)))
        }
    }

    override suspend fun createScene(
        name: String,
        description: String?,
        isDefault: Boolean,
    ): Result<CloudScene> = runCatching {
        val response = request(
            "studio/scenes",
            method = "POST",
            authenticated = true,
            body = """{"name":${jsonString(name)},"description":${jsonString(description)},"isDefault":$isDefault}""",
        )
        sceneFrom(JSONObject(response))
    }

    override suspend fun updateScene(
        id: String,
        name: String?,
        isDefault: Boolean?,
    ): Result<CloudScene> = runCatching {
        val fields = mutableListOf<String>()
        if (name != null) fields += """"name":${jsonString(name)}"""
        if (isDefault != null) fields += """"isDefault":$isDefault"""
        val response = request(
            "studio/scenes/$id",
            method = "PATCH",
            authenticated = true,
            body = "{${fields.joinToString(",")}}",
        )
        sceneFrom(JSONObject(response))
    }

    override suspend fun duplicateScene(id: String): Result<CloudScene> = runCatching {
        val response = request("studio/scenes/$id/duplicate", method = "POST", body = "{}", authenticated = true)
        sceneFrom(JSONObject(response))
    }

    override suspend fun deleteScene(id: String): Result<Unit> = runCatching {
        request("studio/scenes/$id", method = "DELETE", authenticated = true)
        Unit
    }

    override suspend fun sceneSources(sceneId: String): Result<List<CloudSceneSource>> = runCatching {
        val response = request("studio/scenes/$sceneId/sources", authenticated = true)
        val array = org.json.JSONArray(response)
        buildList {
            for (i in 0 until array.length()) {
                val json = array.getJSONObject(i)
                add(
                    CloudSceneSource(
                        id = json.optString("id"),
                        sceneId = json.optString("scene_id"),
                        sourceType = json.optString("source_type"),
                        name = json.optString("name"),
                        zIndex = json.optInt("z_index", 0),
                        isVisible = json.optBoolean("is_visible", true),
                        isLocked = json.optBoolean("is_locked", false),
                        x = json.optDouble("x", 0.0),
                        y = json.optDouble("y", 0.0),
                        width = json.optDouble("width", 1.0),
                        height = json.optDouble("height", 1.0),
                        rotation = json.optDouble("rotation", 0.0),
                        opacity = json.optDouble("opacity", 1.0),
                    )
                )
            }
        }
    }


    private fun broadcastFrom(json: JSONObject) = BroadcastSession(
        id = json.optString("id"),
        title = json.optString("title").takeIf { it.isNotBlank() && it != "null" },
        status = json.optString("status", "created"),
        startedAt = json.optString("started_at").takeIf { it.isNotBlank() && it != "null" },
        endedAt = json.optString("ended_at").takeIf { it.isNotBlank() && it != "null" },
    )

    override suspend fun createBroadcastSession(
        title: String,
        connectionIds: List<String>,
        sceneId: String?,
    ): Result<BroadcastSession> = runCatching {
        val destinations = connectionIds.joinToString(",") { id ->
            """{"connectionId":${jsonString(id)},"platform":"saved"}"""
        }
        val response = request(
            "broadcast/sessions",
            method = "POST",
            authenticated = true,
            body = """
                {
                  "title":${jsonString(title)},
                  "sceneId":${jsonString(sceneId)},
                  "destinations":[$destinations]
                }
            """.trimIndent(),
        )
        broadcastFrom(JSONObject(response))
    }

    override suspend fun startBroadcastSession(id: String): Result<BroadcastSession> = runCatching {
        val response = request(
            "broadcast/sessions/$id/start",
            method = "POST",
            body = "{}",
            authenticated = true,
        )
        broadcastFrom(JSONObject(response))
    }

    override suspend fun heartbeatBroadcastSession(id: String): Result<Unit> = runCatching {
        request(
            "broadcast/sessions/$id/heartbeat",
            method = "POST",
            body = "{}",
            authenticated = true,
        )
        Unit
    }

    override suspend fun endBroadcastSession(id: String): Result<BroadcastSession> = runCatching {
        val response = request(
            "broadcast/sessions/$id/end",
            method = "POST",
            body = "{}",
            authenticated = true,
        )
        broadcastFrom(JSONObject(response))
    }

    override suspend fun sendTelemetry(
        sessionId: String,
        bitrateKbps: Int?,
        fps: Double?,
        droppedFrames: Int?,
        networkStatus: String?,
    ): Result<Unit> = runCatching {
        val fields = mutableListOf<String>()
        bitrateKbps?.let { fields += """"bitrateKbps":$it""" }
        fps?.let { fields += """"fps":$it""" }
        droppedFrames?.let { fields += """"droppedFrames":$it""" }
        networkStatus?.let { fields += """"networkStatus":${jsonString(it)}""" }

        request(
            "telemetry/sessions/$sessionId",
            method = "POST",
            body = "{${fields.joinToString(",")}}",
            authenticated = true,
        )
        Unit
    }

    override suspend fun streamHistory(): Result<List<StreamHistoryItem>> = runCatching {
        val response = request("history/streams", authenticated = true)
        val array = org.json.JSONArray(response)
        buildList {
            for (i in 0 until array.length()) {
                val json = array.getJSONObject(i)
                add(
                    StreamHistoryItem(
                        id = json.optString("id"),
                        title = json.optString("title").takeIf { it.isNotBlank() && it != "null" },
                        status = json.optString("status"),
                        startedAt = json.optString("started_at").takeIf { it.isNotBlank() && it != "null" },
                        endedAt = json.optString("ended_at").takeIf { it.isNotBlank() && it != "null" },
                        durationSeconds = json.optInt("duration_seconds").takeIf { json.has("duration_seconds") && !json.isNull("duration_seconds") },
                        avgBitrateKbps = json.optInt("avg_bitrate_kbps").takeIf { json.has("avg_bitrate_kbps") && !json.isNull("avg_bitrate_kbps") },
                        avgFps = json.optDouble("avg_fps").takeIf { json.has("avg_fps") && !json.isNull("avg_fps") },
                        droppedFrames = json.optInt("dropped_frames", 0),
                    )
                )
            }
        }
    }

    override suspend fun notifications(): Result<List<AppNotification>> = runCatching {
        val response = request("notifications", authenticated = true)
        val array = org.json.JSONArray(response)
        buildList {
            for (i in 0 until array.length()) {
                val json = array.getJSONObject(i)
                add(
                    AppNotification(
                        id = json.optString("id"),
                        title = json.optString("title"),
                        body = json.optString("body"),
                        isRead = json.optBoolean("is_read", false),
                        createdAt = json.optString("created_at").takeIf { it.isNotBlank() && it != "null" },
                    )
                )
            }
        }
    }

    override suspend fun markNotificationRead(id: String): Result<Unit> = runCatching {
        request(
            "notifications/$id/read",
            method = "POST",
            body = "{}",
            authenticated = true,
        )
        Unit
    }

    override suspend fun supportTickets(): Result<List<SupportTicket>> = runCatching {
        val response = request("support/tickets", authenticated = true)
        val array = org.json.JSONArray(response)
        buildList {
            for (i in 0 until array.length()) {
                val json = array.getJSONObject(i)
                add(
                    SupportTicket(
                        id = json.optString("id"),
                        category = json.optString("category"),
                        subject = json.optString("subject"),
                        description = json.optString("description"),
                        status = json.optString("status"),
                        priority = json.optString("priority"),
                        createdAt = json.optString("created_at").takeIf { it.isNotBlank() && it != "null" },
                    )
                )
            }
        }
    }

    override suspend fun createSupportTicket(
        category: String,
        subject: String,
        description: String,
    ): Result<SupportTicket> = runCatching {
        val response = request(
            "support/tickets",
            method = "POST",
            authenticated = true,
            body = """
                {
                  "category":${jsonString(category)},
                  "subject":${jsonString(subject)},
                  "description":${jsonString(description)}
                }
            """.trimIndent(),
        )
        val json = JSONObject(response)
        SupportTicket(
            id = json.optString("id"),
            category = json.optString("category"),
            subject = json.optString("subject"),
            description = json.optString("description"),
            status = json.optString("status"),
            priority = json.optString("priority"),
            createdAt = json.optString("created_at").takeIf { it.isNotBlank() && it != "null" },
        )
    }
}
