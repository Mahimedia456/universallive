package com.universallive.app.integration

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.universallive.app.BuildConfig
import com.universallive.app.push.PushTokenStore
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

    private val appContext = context.applicationContext

    override val baseUrl: String =
        BuildConfig.UNIVERSALLIVE_API_BASE_URL.trimEnd('/')

    private val prefs =
        appContext.getSharedPreferences("universallive_session", Context.MODE_PRIVATE)

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
        val parsed = runCatching {
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

        return if (
            code == 404 &&
            (parsed.contains("Cannot PUT /api/v1/streams/", ignoreCase = true) ||
                parsed.contains("Cannot POST /api/v1/streams/", ignoreCase = true))
        ) {
            "This app is connected to an older backend deployment. Deploy/restart the final Universal Live backend, then run preflight again."
        } else {
            parsed
        }
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
                body = """{"refreshToken":${jsonString(currentSession?.refreshToken)}}""",
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
        creatorType: String?,
    ): Result<CreatorProfile> = runCatching {
        val onboardingPart = onboardingCompleted?.let { ""","onboardingCompleted":$it""" } ?: ""
        val creatorTypePart = creatorType?.takeIf { it.isNotBlank() }
            ?.let { ""","creatorType":${jsonString(it)}""" } ?: ""
        val response = request(
            "profiles/me",
            method = "PUT",
            body = """
                {
                  "displayName":${jsonString(displayName)},
                  "username":${jsonString(username.removePrefix("@"))}
                  $onboardingPart
                  $creatorTypePart
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


    override suspend fun billingPlans(): Result<List<BillingPlanInfo>> = runCatching {
        val response = request("billing/plans")
        val array = org.json.JSONArray(response)
        buildList {
            for (i in 0 until array.length()) {
                val json = array.getJSONObject(i)
                add(
                    BillingPlanInfo(
                        planKey = json.optString("plan_key"),
                        name = json.optString("name"),
                        description = json.optString("description").takeIf { it.isNotBlank() && it != "null" },
                        monthlyProductId = json.optString("monthly_product_id").takeIf { it.isNotBlank() && it != "null" },
                        yearlyProductId = json.optString("yearly_product_id").takeIf { it.isNotBlank() && it != "null" },
                        displayMonthly = json.optString("display_monthly").takeIf { it.isNotBlank() && it != "null" },
                        displayYearly = json.optString("display_yearly").takeIf { it.isNotBlank() && it != "null" },
                    )
                )
            }
        }
    }

    override suspend fun registerDevice(): Result<Unit> = runCatching {
        val deviceId = Settings.Secure.getString(
            appContext.contentResolver,
            Settings.Secure.ANDROID_ID,
        ).orEmpty().ifBlank { "android-unknown" }
        val versionName = runCatching {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName
        }.getOrNull().orEmpty()
        val locale = java.util.Locale.getDefault().toLanguageTag()
        val timezone = java.util.TimeZone.getDefault().id

        request(
            "devices/register",
            method = "POST",
            authenticated = true,
            body = """
                {
                  "deviceId":${jsonString(deviceId)},
                  "platform":"android",
                  "pushToken":${jsonString(PushTokenStore.read(appContext))},
                  "appVersion":${jsonString(versionName)},
                  "osVersion":${jsonString(Build.VERSION.RELEASE)},
                  "deviceModel":${jsonString("${Build.MANUFACTURER} ${Build.MODEL}".trim())},
                  "locale":${jsonString(locale)},
                  "timezone":${jsonString(timezone)}
                }
            """.trimIndent(),
        )
        Unit
    }

    override suspend fun homeDashboard(): Result<HomeDashboard> = runCatching {
        val json = JSONObject(request("home/dashboard", authenticated = true))
        val readiness = json.optJSONObject("readiness") ?: JSONObject()
        val destination = json.optJSONObject("default_destination")
        val scene = json.optJSONObject("default_scene")
        val config = json.optJSONObject("stream_config") ?: JSONObject()
        val active = json.optJSONObject("active_broadcast")
        HomeDashboard(
            readyToStream = readiness.optBoolean("ready_to_stream", false),
            destinationCount = readiness.optInt("destination_count", 0),
            readyDestinationCount = readiness.optInt("ready_destination_count", 0),
            defaultDestinationId = destination?.optString("id")?.takeIf { it.isNotBlank() && it != "null" },
            defaultSceneName = scene?.optString("name")?.takeIf { it.isNotBlank() && it != "null" },
            resolution = config.optString("resolution", "1080p"),
            fps = config.optInt("fps", 30),
            bitrateKbps = config.optInt("bitrate_kbps", 6800),
            activeBroadcastId = active?.optString("id")?.takeIf { it.isNotBlank() && it != "null" },
            activeBroadcastStatus = active?.optString("status")?.takeIf { it.isNotBlank() && it != "null" },
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
            creatorContentTypes = jsonStringSet(json, "creator_content_types"),
            preferredPlatforms = jsonStringSet(json, "preferred_platforms"),
            experienceLevel = json.optString("experience_level").takeIf { it.isNotBlank() && it != "null" },
            primaryGoal = json.optString("primary_goal").takeIf { it.isNotBlank() && it != "null" },
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
                  "notifications_acknowledged":${state.notificationsAcknowledged},
                  "creator_content_types":${org.json.JSONArray(state.creatorContentTypes.toList())},
                  "preferred_platforms":${org.json.JSONArray(state.preferredPlatforms.toList())},
                  "experience_level":${jsonString(state.experienceLevel)},
                  "primary_goal":${jsonString(state.primaryGoal)}
                }
            """.trimIndent(),
        )
        state
    }


    private fun jsonStringSet(json: JSONObject, key: String): Set<String> {
        val array = json.optJSONArray(key) ?: return emptySet()
        return buildSet {
            for (i in 0 until array.length()) {
                val value = array.optString(i).trim()
                if (value.isNotBlank()) add(value)
            }
        }
    }

    private fun connectionFrom(json: JSONObject) = StreamingConnection(
        id = json.optString("id"),
        platform = json.optString("platform"),
        displayName = json.optString("display_name"),
        status = json.optString("status", "disconnected"),
        isDefault = json.optBoolean("is_default", false),
        isEnabled = json.optBoolean("is_enabled", true),
        credentialConfigured = json.optBoolean("credential_configured", false),
        readyToPublish = json.optBoolean("ready_to_publish", false),
        credentialUpdatedAt = json.optString("credential_updated_at").takeIf { it.isNotBlank() && it != "null" },
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

    override suspend fun publishConfig(connectionId: String): Result<PublishConfig> = runCatching {
        val response = request(
            "streaming/rtmp/$connectionId/publish-config",
            authenticated = true,
        )
        val json = JSONObject(response)
        PublishConfig(
            connectionId = json.optString("connectionId", connectionId),
            platform = json.optString("platform"),
            displayName = json.optString("displayName"),
            serverUrl = json.optString("serverUrl"),
            streamKey = json.optString("streamKey"),
        )
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


    private fun sourceFrom(json: JSONObject) = CloudSceneSource(
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

    private fun audioFrom(json: JSONObject) = StudioAudioConfig(
        microphoneEnabled = json.optBoolean("microphoneEnabled", true),
        internalAudioEnabled = json.optBoolean("internalAudioEnabled", true),
        microphoneGain = json.optDouble("microphoneGain", 1.0),
        internalAudioGain = json.optDouble("internalAudioGain", 1.0),
        monitoringEnabled = json.optBoolean("monitoringEnabled", false),
        preset = json.optString("preset", "Streaming"),
    )

    private fun facecamFrom(json: JSONObject) = StudioFacecamConfig(
        enabled = json.optBoolean("enabled", false),
        lens = json.optString("lens", "front"),
        shape = json.optString("shape", "rounded"),
        size = json.optDouble("size", 0.25),
        x = json.optDouble("x", 0.72),
        y = json.optDouble("y", 0.05),
        mirror = json.optBoolean("mirror", true),
        background = json.optString("background", "None"),
    )

    override suspend fun studioWorkspace(): Result<StudioWorkspaceSnapshot> = runCatching {
        val json = JSONObject(request("studio/workspace", authenticated = true))
        StudioWorkspaceSnapshot(
            activeSceneId = json.optString("active_scene_id").takeIf { it.isNotBlank() && it != "null" },
            qualityConfigId = json.optString("quality_config_id").takeIf { it.isNotBlank() && it != "null" },
            audio = audioFrom(json.optJSONObject("audio_config") ?: JSONObject()),
            facecam = facecamFrom(json.optJSONObject("facecam_config") ?: JSONObject()),
            adaptiveBitrateEnabled = json.optBoolean("adaptive_bitrate_enabled", true),
            autosaveEnabled = json.optBoolean("autosave_enabled", true),
        )
    }

    override suspend fun activateScene(id: String): Result<Unit> = runCatching {
        request("studio/scenes/$id/activate", method = "PUT", body = "{}", authenticated = true)
        Unit
    }

    override suspend fun saveStudioAudio(config: StudioAudioConfig): Result<StudioAudioConfig> = runCatching {
        val body = JSONObject()
            .put("microphoneEnabled", config.microphoneEnabled)
            .put("internalAudioEnabled", config.internalAudioEnabled)
            .put("microphoneGain", config.microphoneGain)
            .put("internalAudioGain", config.internalAudioGain)
            .put("monitoringEnabled", config.monitoringEnabled)
            .put("preset", config.preset)
        audioFrom(JSONObject(request("studio/audio", method = "PUT", body = body.toString(), authenticated = true)))
    }

    override suspend fun saveStudioFacecam(config: StudioFacecamConfig): Result<StudioFacecamConfig> = runCatching {
        val body = JSONObject()
            .put("enabled", config.enabled)
            .put("lens", config.lens)
            .put("shape", config.shape)
            .put("size", config.size)
            .put("x", config.x)
            .put("y", config.y)
            .put("mirror", config.mirror)
            .put("background", config.background)
        facecamFrom(JSONObject(request("studio/facecam", method = "PUT", body = body.toString(), authenticated = true)))
    }

    private fun qualityFrom(json: JSONObject): StudioQualityConfig = StudioQualityConfig(
        id = json.optString("id").takeIf { it.isNotBlank() && it != "null" },
        resolution = json.optString("resolution", "1080p"),
        width = json.optInt("width", 1920),
        height = json.optInt("height", 1080),
        fps = json.optInt("fps", 30),
        bitrateKbps = json.optInt("bitrate_kbps", 6800),
        orientation = json.optString("orientation", "auto"),
        keyframeIntervalSeconds = json.optInt("keyframe_interval_seconds", 2),
        audioBitrateKbps = json.optInt("audio_bitrate_kbps", 160),
        audioSampleRateHz = json.optInt("audio_sample_rate_hz", 48000),
        adaptiveBitrateEnabled = json.optBoolean("adaptive_bitrate_enabled", true),
    )

    override suspend fun studioQuality(): Result<StudioQualityConfig?> = runCatching {
        val text = request("studio/quality", authenticated = true)
        if (text.isBlank() || text == "null") null else qualityFrom(JSONObject(text))
    }

    override suspend fun saveStudioQuality(config: StudioQualityConfig): Result<StudioQualityConfig> = runCatching {
        val body = JSONObject()
            .put("resolution", config.resolution)
            .put("width", config.width)
            .put("height", config.height)
            .put("fps", config.fps)
            .put("bitrateKbps", config.bitrateKbps)
            .put("orientation", config.orientation)
            .put("keyframeIntervalSeconds", config.keyframeIntervalSeconds)
            .put("audioBitrateKbps", config.audioBitrateKbps)
            .put("audioSampleRateHz", config.audioSampleRateHz)
            .put("adaptiveBitrateEnabled", config.adaptiveBitrateEnabled)
        qualityFrom(JSONObject(request("studio/quality", method = "PUT", body = body.toString(), authenticated = true)))
    }

    override suspend fun upsertSceneSource(
        sceneId: String,
        sourceKey: String,
        sourceType: String,
        name: String,
        configJson: String,
    ): Result<CloudSceneSource> = runCatching {
        val config = runCatching { JSONObject(configJson) }.getOrElse { JSONObject() }
        val body = JSONObject().put("sourceType", sourceType).put("name", name).put("config", config)
        sourceFrom(JSONObject(request("studio/scenes/$sceneId/sources/key/$sourceKey", method = "PUT", body = body.toString(), authenticated = true)))
    }

    override suspend fun updateSceneSource(
        sourceId: String,
        isVisible: Boolean?,
        zIndex: Int?,
        configJson: String?,
    ): Result<CloudSceneSource> = runCatching {
        val body = JSONObject()
        isVisible?.let { body.put("isVisible", it) }
        zIndex?.let { body.put("zIndex", it) }
        configJson?.let { body.put("config", runCatching { JSONObject(it) }.getOrElse { JSONObject() }) }
        sourceFrom(JSONObject(request("studio/sources/$sourceId", method = "PATCH", body = body.toString(), authenticated = true)))
    }

    override suspend fun deleteSceneSource(sourceId: String): Result<Unit> = runCatching {
        request("studio/sources/$sourceId", method = "DELETE", authenticated = true)
        Unit
    }


    private fun broadcastFrom(json: JSONObject) = BroadcastSession(
        id = json.optString("id"),
        title = json.optString("title").takeIf { it.isNotBlank() && it != "null" },
        status = json.optString("status", "created"),
        startedAt = json.optString("started_at").takeIf { it.isNotBlank() && it != "null" },
        endedAt = json.optString("ended_at").takeIf { it.isNotBlank() && it != "null" },
    )

    override suspend fun saveStreamDraft(
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
    ): Result<Unit> = runCatching {
        val body = JSONObject()
            .put("title", title)
            .put("description", description)
            .put("category", category)
            .put("privacy", privacy.lowercase())
            .put("connectionId", connectionId)
            .put("sceneId", sceneId ?: JSONObject.NULL)
            .put(
                "config",
                JSONObject()
                    .put("width", width)
                    .put("height", height)
                    .put("fps", fps)
                    .put("bitrateKbps", bitrateKbps)
                    .put("microphoneEnabled", microphoneEnabled)
                    .put("internalAudioEnabled", internalAudioEnabled)
                    .put("orientation", orientation)
                    .put("keyframeIntervalSeconds", 2)
                    .put("audioSampleRateHz", 48_000)
                    .put("audioBitrateKbps", 160),
            )
        request(
            "streams/draft/current",
            method = "PUT",
            authenticated = true,
            body = body.toString(),
        )
        Unit
    }

    override suspend fun runStreamPreflight(
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
    ): Result<StreamPreflightResult> = runCatching {
        val body = JSONObject()
            .put("title", title)
            .put("connectionIds", org.json.JSONArray(connectionIds))
            .put("sceneId", sceneId ?: JSONObject.NULL)
            .put(
                "config",
                JSONObject()
                    .put("width", width)
                    .put("height", height)
                    .put("fps", fps)
                    .put("bitrateKbps", bitrateKbps)
                    .put("microphoneEnabled", microphoneEnabled)
                    .put("internalAudioEnabled", internalAudioEnabled)
                    .put("orientation", orientation)
                    .put("keyframeIntervalSeconds", 2)
                    .put("audioSampleRateHz", 48_000)
                    .put("audioBitrateKbps", 160),
            )
        val json = JSONObject(
            request(
                "streams/preflight",
                method = "POST",
                authenticated = true,
                body = body.toString(),
            )
        )
        val warnings = buildList {
            val array = json.optJSONArray("warnings") ?: org.json.JSONArray()
            for (i in 0 until array.length()) add(array.optString(i))
        }
        StreamPreflightResult(
            id = json.optString("id"),
            ready = json.optBoolean("ready", false),
            status = json.optString("status", "failed"),
            expiresAt = json.optString("expiresAt").takeIf { it.isNotBlank() && it != "null" },
            warnings = warnings,
        )
    }

    override suspend fun createBroadcastSession(
        title: String,
        connectionIds: List<String>,
        sceneId: String?,
        preflightId: String?,
    ): Result<BroadcastSession> = runCatching {
        val destinations = org.json.JSONArray().apply {
            connectionIds.forEach { id ->
                put(JSONObject().put("connectionId", id).put("platform", "saved"))
            }
        }
        val body = JSONObject()
            .put("title", title)
            .put("sceneId", sceneId ?: JSONObject.NULL)
            .put("preflightId", preflightId ?: JSONObject.NULL)
            .put("destinations", destinations)
        val response = request(
            "streams/sessions",
            method = "POST",
            authenticated = true,
            body = body.toString(),
        )
        broadcastFrom(JSONObject(response))
    }

    override suspend fun startBroadcastSession(id: String): Result<BroadcastSession> = runCatching {
        val response = request(
            "streams/sessions/$id/start",
            method = "POST",
            body = "{}",
            authenticated = true,
        )
        broadcastFrom(JSONObject(response))
    }

    override suspend fun activeBroadcastSession(): Result<BroadcastSession?> = runCatching {
        val response = request(
            "streams/sessions/active/current",
            authenticated = true,
        ).trim()
        if (response.isBlank() || response == "null") null else broadcastFrom(JSONObject(response))
    }

    override suspend fun heartbeatBroadcastSession(id: String): Result<Unit> = runCatching {
        request(
            "streams/sessions/$id/heartbeat",
            method = "POST",
            body = "{}",
            authenticated = true,
        )
        Unit
    }

    override suspend fun reportPublisherState(
        sessionId: String,
        telemetry: DeviceStreamTelemetry,
    ): Result<BroadcastSession> = runCatching {
        val response = request(
            "streams/sessions/$sessionId/publisher-state",
            method = "POST",
            body = telemetryJson(telemetry).toString(),
            authenticated = true,
        )
        val json = JSONObject(response)
        broadcastFrom(json.optJSONObject("session") ?: json)
    }

    override suspend fun recoverBroadcastSession(
        id: String,
        publisherInstanceId: String?,
    ): Result<StreamRecoveryResult> = runCatching {
        val body = JSONObject().put("publisherInstanceId", publisherInstanceId ?: JSONObject.NULL)
        val json = JSONObject(
            request(
                "streams/sessions/$id/recover",
                method = "POST",
                body = body.toString(),
                authenticated = true,
            )
        )
        val destination = json.optJSONArray("destinations")?.optJSONObject(0)
        val configJson = destination?.optJSONObject("publishConfig")
        val config = configJson?.let {
            PublishConfig(
                connectionId = it.optString("connectionId"),
                platform = it.optString("platform", "custom_rtmp"),
                displayName = it.optString("displayName", "Destination"),
                serverUrl = it.optString("serverUrl"),
                streamKey = it.optString("streamKey"),
            )
        }
        StreamRecoveryResult(
            session = broadcastFrom(json.getJSONObject("session")),
            publishConfig = config,
            recoveryAttempt = json.optInt("recoveryAttempt", 0),
            recoverableUntil = json.optString("recoverableUntil").takeIf { it.isNotBlank() && it != "null" },
        )
    }

    override suspend fun streamDiagnostics(id: String): Result<StreamDiagnostics> = runCatching {
        val json = JSONObject(request("streams/sessions/$id/diagnostics", authenticated = true))
        val issues = buildList {
            val array = json.optJSONArray("issues") ?: org.json.JSONArray()
            for (i in 0 until array.length()) add(array.optString(i))
        }
        StreamDiagnostics(
            health = json.optString("health", "starting"),
            issues = issues,
        )
    }

    override suspend fun endBroadcastSession(id: String): Result<BroadcastSession> = runCatching {
        val response = request(
            "streams/sessions/$id/end",
            method = "POST",
            body = "{}",
            authenticated = true,
        )
        broadcastFrom(JSONObject(response))
    }

    override suspend fun sendTelemetry(
        sessionId: String,
        telemetry: DeviceStreamTelemetry,
    ): Result<Unit> = runCatching {
        request(
            "streams/sessions/$sessionId/telemetry",
            method = "POST",
            body = telemetryJson(telemetry).toString(),
            authenticated = true,
        )
        Unit
    }

    private fun telemetryJson(value: DeviceStreamTelemetry): JSONObject = JSONObject().apply {
        value.connectionId?.let { put("connectionId", it) }
        value.bitrateKbps?.let { put("bitrateKbps", it) }
        value.targetBitrateKbps?.let { put("targetBitrateKbps", it) }
        value.encoderBitrateKbps?.let { put("encoderBitrateKbps", it) }
        value.rtmpUploadKbps?.let { put("rtmpUploadKbps", it) }
        value.fps?.let { put("fps", it) }
        value.encodedFps?.let { put("encodedFps", it) }
        value.sentFps?.let { put("sentFps", it) }
        value.droppedFrames?.let { put("droppedFrames", it) }
        value.publishedVideoFrames?.let { put("publishedVideoFrames", it) }
        value.publishedAudioFrames?.let { put("publishedAudioFrames", it) }
        value.encoderWidth?.let { put("encoderWidth", it) }
        value.encoderHeight?.let { put("encoderHeight", it) }
        value.encoderName?.let { put("encoderName", it) }
        value.networkStatus?.let { put("networkStatus", it) }
        value.publishStatus?.let { put("publishStatus", it) }
        value.audioStatus?.let { put("audioStatus", it) }
        value.rtmpQueueDepth?.let { put("rtmpQueueDepth", it) }
        value.socketWriteLatencyMs?.let { put("socketWriteLatencyMs", it) }
        value.publisherEnqueueLatencyMs?.let { put("publisherEnqueueLatencyMs", it) }
        value.lastVideoPacketAgeMs?.let { put("lastVideoPacketAgeMs", it) }
        value.lastAudioPacketAgeMs?.let { put("lastAudioPacketAgeMs", it) }
        value.keyframeIntervalMs?.let { put("keyframeIntervalMs", it) }
        value.videoPtsMonotonic?.let { put("videoPtsMonotonic", it) }
        value.audioPtsMonotonic?.let { put("audioPtsMonotonic", it) }
        value.reconnectCount?.let { put("reconnectCount", it) }
        value.publisherInstanceId?.let { put("publisherInstanceId", it) }
    }

    private fun historyItemFrom(json: JSONObject): StreamHistoryItem {
        val summary = json.optJSONObject("summary")
        return StreamHistoryItem(
            id = json.optString("id"),
            title = json.optString("title").takeIf { it.isNotBlank() && it != "null" },
            status = json.optString("status"),
            startedAt = json.optString("started_at").takeIf { it.isNotBlank() && it != "null" },
            endedAt = json.optString("ended_at").takeIf { it.isNotBlank() && it != "null" },
            durationSeconds = summary?.optInt("duration_seconds")?.takeIf { summary.has("duration_seconds") && !summary.isNull("duration_seconds") }
                ?: json.optInt("duration_seconds").takeIf { json.has("duration_seconds") && !json.isNull("duration_seconds") },
            avgBitrateKbps = summary?.optInt("avg_bitrate_kbps")?.takeIf { summary.has("avg_bitrate_kbps") && !summary.isNull("avg_bitrate_kbps") }
                ?: json.optInt("avg_bitrate_kbps").takeIf { json.has("avg_bitrate_kbps") && !json.isNull("avg_bitrate_kbps") },
            avgFps = summary?.optDouble("avg_fps")?.takeIf { summary.has("avg_fps") && !summary.isNull("avg_fps") }
                ?: json.optDouble("avg_fps").takeIf { json.has("avg_fps") && !json.isNull("avg_fps") },
            droppedFrames = summary?.optInt("dropped_frames", 0) ?: json.optInt("dropped_frames", 0),
        )
    }

    private suspend fun loadHistory(path: String): List<StreamHistoryItem> {
        val array = org.json.JSONArray(request(path, authenticated = true))
        return buildList {
            for (i in 0 until array.length()) add(historyItemFrom(array.getJSONObject(i)))
        }
    }

    override suspend fun streamHistory(): Result<List<StreamHistoryItem>> = runCatching {
        loadHistory("streams/history?limit=50&offset=0")
    }

    override suspend fun streamHistoryFiltered(status: String?, limit: Int, offset: Int): Result<List<StreamHistoryItem>> = runCatching {
        val query = buildString {
            append("streams/history?limit=").append(limit.coerceIn(1, 100)).append("&offset=").append(offset.coerceAtLeast(0))
            if (!status.isNullOrBlank() && !status.equals("all", true)) append("&status=").append(java.net.URLEncoder.encode(status, "UTF-8"))
        }
        loadHistory(query)
    }

    override suspend fun streamAnalytics(sessionId: String): Result<StreamAnalyticsDetail> = runCatching {
        val root = JSONObject(request("streams/history/$sessionId/analytics", authenticated = true))
        val a = root.optJSONObject("analytics") ?: JSONObject()
        StreamAnalyticsDetail(
            sessionId = sessionId,
            healthGrade = a.optString("healthGrade", "unknown"),
            durationSeconds = a.optInt("durationSeconds").takeIf { a.has("durationSeconds") && !a.isNull("durationSeconds") },
            sampleCount = a.optInt("sampleCount", 0),
            avgBitrateKbps = a.optInt("avgBitrateKbps").takeIf { a.has("avgBitrateKbps") && !a.isNull("avgBitrateKbps") },
            minBitrateKbps = a.optInt("minBitrateKbps").takeIf { a.has("minBitrateKbps") && !a.isNull("minBitrateKbps") },
            peakBitrateKbps = a.optInt("peakBitrateKbps").takeIf { a.has("peakBitrateKbps") && !a.isNull("peakBitrateKbps") },
            avgRtmpUploadKbps = a.optInt("avgRtmpUploadKbps").takeIf { a.has("avgRtmpUploadKbps") && !a.isNull("avgRtmpUploadKbps") },
            avgFps = a.optDouble("avgFps").takeIf { a.has("avgFps") && !a.isNull("avgFps") },
            avgEncodedFps = a.optDouble("avgEncodedFps").takeIf { a.has("avgEncodedFps") && !a.isNull("avgEncodedFps") },
            avgSentFps = a.optDouble("avgSentFps").takeIf { a.has("avgSentFps") && !a.isNull("avgSentFps") },
            droppedFrames = a.optInt("droppedFrames", 0),
            reconnectCount = a.optInt("reconnectCount", 0),
            warningCount = a.optInt("warningCount", 0),
            errorCount = a.optInt("errorCount", 0),
        )
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
                        actionRoute = json.optJSONObject("action_payload")?.optString("route")?.takeIf { it.isNotBlank() },
                    )
                )
            }
        }
    }

    override suspend fun notificationUnreadCount(): Result<Int> = runCatching {
        JSONObject(request("notifications/unread-count", authenticated = true)).optInt("unread", 0)
    }

    override suspend fun markNotificationRead(id: String): Result<Unit> = runCatching {
        request(
            "notifications/$id/read",
            method = "PATCH",
            body = "{}",
            authenticated = true,
        )
        Unit
    }

    override suspend fun markAllNotificationsRead(): Result<Unit> = runCatching {
        request("notifications/read-all", method = "POST", body = "{}", authenticated = true)
        Unit
    }

    override suspend fun sendTestPush(): Result<String> = runCatching {
        val json = JSONObject(request("notifications/push/test", method = "POST", body = "{}", authenticated = true))
        val delivery = json.optJSONObject("delivery")
        "sent=${delivery?.optInt("sent", 0) ?: 0}, failed=${delivery?.optInt("failed", 0) ?: 0}"
    }

    override suspend fun settings(): Result<UserSettings> = runCatching {
        parseUserSettings(JSONObject(request("settings", authenticated = true)))
    }

    override suspend fun saveStreamingSettings(streamDefaultsJson: String): Result<UserSettings> = runCatching {
        val streamJson = runCatching { JSONObject(streamDefaultsJson) }.getOrElse { JSONObject() }
        val response = request(
            "settings/streaming",
            method = "PUT",
            body = streamJson.toString(),
            authenticated = true,
        )
        parseUserSettings(JSONObject(response))
    }

    override suspend fun accountSessions(): Result<List<AccountSessionInfo>> = runCatching {
        val array = org.json.JSONArray(request("account/sessions", authenticated = true))
        buildList {
            for (i in 0 until array.length()) {
                val json = array.getJSONObject(i)
                add(AccountSessionInfo(
                    id = json.optString("id"),
                    userAgent = json.optString("user_agent").takeIf { it.isNotBlank() && it != "null" },
                    active = json.optBoolean("active", false),
                    createdAt = json.optString("created_at").takeIf { it.isNotBlank() && it != "null" },
                    lastUsedAt = json.optString("last_used_at").takeIf { it.isNotBlank() && it != "null" },
                    expiresAt = json.optString("expires_at").takeIf { it.isNotBlank() && it != "null" },
                ))
            }
        }
    }

    override suspend fun logoutAllSessions(): Result<Unit> = runCatching {
        request("account/logout-all", method = "POST", body = "{}", authenticated = true)
        saveSession(null)
        Unit
    }

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        request("account", method = "DELETE", body = "{\"confirmation\":\"DELETE\"}", authenticated = true)
        saveSession(null)
        Unit
    }

    private fun parseUserSettings(json: JSONObject): UserSettings {
        val stream = json.optJSONObject("stream_defaults") ?: json.optJSONObject("streamDefaults") ?: JSONObject()
        return UserSettings(
            notificationsEnabled = json.optBoolean("notifications_enabled", true),
            marketingNotificationsEnabled = json.optBoolean("marketing_notifications_enabled", false),
            streamDefaultsJson = stream.toString(),
        )
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

    override suspend fun diagnosticsSummary(): Result<DiagnosticSummary> = runCatching {
        val json = JSONObject(request("diagnostics/summary", authenticated = true))
        val backend = json.optJSONObject("backend") ?: JSONObject()
        val counts = json.optJSONObject("counts") ?: JSONObject()
        val contract = json.optJSONObject("contract") ?: JSONObject()
        DiagnosticSummary(
            ok = json.optBoolean("ok", false),
            generatedAt = json.optString("generatedAt").takeIf { it.isNotBlank() && it != "null" },
            firebaseConfigured = backend.optBoolean("firebaseConfigured", false),
            smtpConfigured = backend.optBoolean("smtpConfigured", false),
            connectionCount = counts.optInt("connections", 0),
            sceneCount = counts.optInt("scenes", 0),
            activeSessionCount = counts.optInt("activeSessions", 0),
            deviceCount = counts.optInt("devices", 0),
            unreadNotificationCount = counts.optInt("unreadNotifications", 0),
            mobileContractVersion = contract.optString("mobileContractVersion", "2026.09-final"),
        )
    }

    override suspend fun createDiagnosticSnapshot(
        appVersion: String,
        buildNumber: String,
        captureStatus: String,
        publishStatus: String,
    ): Result<String> = runCatching {
        val body = JSONObject()
            .put("appVersion", appVersion)
            .put("buildNumber", buildNumber)
            .put("platform", "android")
            .put("deviceModel", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
            .put("osVersion", Build.VERSION.RELEASE)
            .put("captureStatus", captureStatus)
            .put("publishStatus", publishStatus)
        val json = JSONObject(request("diagnostics/snapshot", method = "POST", body = body.toString(), authenticated = true))
        json.optString("id")
    }

    private fun legalDocumentFrom(json: JSONObject) = LegalDocumentInfo(
        documentKey = json.optString("document_key"),
        title = json.optString("title"),
        version = json.optString("version"),
        body = json.optString("body"),
        publicUrl = json.optString("public_url").takeIf { it.isNotBlank() && it != "null" },
        effectiveAt = json.optString("effective_at").takeIf { it.isNotBlank() && it != "null" },
        requiredAcceptance = json.optBoolean("required_acceptance", false),
    )

    override suspend fun legalAbout(): Result<LegalAboutInfo> = runCatching {
        val json = JSONObject(request("legal/about"))
        val array = json.optJSONArray("documents") ?: org.json.JSONArray()
        val docs = buildList {
            for (i in 0 until array.length()) add(legalDocumentFrom(array.getJSONObject(i)))
        }
        LegalAboutInfo(
            appName = json.optString("appName", "Universal Live"),
            appVersion = json.optString("appVersion", "0.40.0"),
            buildNumber = json.optInt("buildNumber", 39),
            apiVersion = json.optString("apiVersion", "v1"),
            mobileContractVersion = json.optString("mobileContractVersion", "2026.09-final"),
            documents = docs,
        )
    }

    override suspend fun legalDocuments(): Result<List<LegalDocumentInfo>> = runCatching {
        val array = org.json.JSONArray(request("legal/documents"))
        buildList { for (i in 0 until array.length()) add(legalDocumentFrom(array.getJSONObject(i))) }
    }

    override suspend fun acceptLegal(documentKey: String, version: String): Result<Unit> = runCatching {
        val body = JSONObject().put("documentKey", documentKey).put("version", version)
        request("legal/accept", method = "POST", body = body.toString(), authenticated = true)
        Unit
    }

    override suspend fun systemState(): Result<SystemStateSnapshot> = runCatching {
        val json = JSONObject(request("system/state"))
        val maintenance = json.optJSONObject("maintenance") ?: JSONObject()
        val minimum = json.optJSONObject("minimumVersion") ?: JSONObject()
        val features = json.optJSONObject("featureFlags") ?: JSONObject()
        SystemStateSnapshot(
            online = json.optBoolean("online", true),
            maintenanceEnabled = maintenance.optBoolean("enabled", false),
            maintenanceMessage = maintenance.optString("message").takeIf { it.isNotBlank() },
            minimumAndroidVersion = minimum.optString("android").takeIf { it.isNotBlank() },
            forceUpdate = minimum.optBoolean("force", false),
            streamingEnabled = features.optBoolean("streaming", true),
            studioEnabled = features.optBoolean("studio", true),
            notificationsEnabled = features.optBoolean("notifications", true),
            billingEnabled = features.optBoolean("billing", true),
            supportEnabled = features.optBoolean("support", true),
        )
    }

    override suspend fun runFinalQa(): Result<FinalQaRun> = runCatching {
        val json = JSONObject(request("qa/smoke", method = "POST", body = "{}", authenticated = true))
        val array = json.optJSONArray("checks") ?: org.json.JSONArray()
        val checks = buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(FinalQaCheck(item.optString("key"), item.optBoolean("pass", false), item.optBoolean("required", false)))
            }
        }
        FinalQaRun(
            id = json.optString("id").takeIf { it.isNotBlank() && it != "null" },
            status = json.optString("status", "review"),
            requiredPassed = json.optBoolean("requiredPassed", false),
            checks = checks,
            generatedAt = json.optString("generatedAt").takeIf { it.isNotBlank() && it != "null" },
        )
    }
}
