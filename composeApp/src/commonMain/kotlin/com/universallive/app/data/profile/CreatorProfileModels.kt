package com.universallive.app.data.profile

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreatorProfileDto(
    @SerialName("user_id")
    val userId: String,
    @SerialName("display_name")
    val displayName: String? = null,
    val username: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("creator_type")
    val creatorType: String? = null,
    @SerialName("onboarding_completed")
    val onboardingCompleted: Boolean = false,
)

@Serializable
data class UpdateCreatorProfileRequest(
    val displayName: String? = null,
    val username: String? = null,
    val avatarUrl: String? = null,
    val creatorType: String? = null,
    val onboardingCompleted: Boolean = false,
)
