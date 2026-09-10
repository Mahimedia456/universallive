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
}
