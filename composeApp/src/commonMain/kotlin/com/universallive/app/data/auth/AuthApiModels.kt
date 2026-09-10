package com.universallive.app.data.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MobileAuthUser(
    val id: String,
    val email: String? = null,
)

@Serializable
data class MobileAuthSession(
    @SerialName("access_token")
    val accessToken: String? = null,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
    @SerialName("expires_in")
    val expiresIn: Long? = null,
    val user: MobileAuthUser? = null,
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val fullName: String? = null,
    val username: String? = null,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class VerifyEmailRequest(
    val email: String,
    val token: String,
)
