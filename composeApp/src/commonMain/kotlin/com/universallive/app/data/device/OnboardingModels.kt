package com.universallive.app.data.device

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OnboardingStateDto(
    @SerialName("creator_setup_completed")
    val creatorSetupCompleted: Boolean = false,
    @SerialName("permission_education_completed")
    val permissionEducationCompleted: Boolean = false,
    @SerialName("first_destination_prompt_completed")
    val firstDestinationPromptCompleted: Boolean = false,
    @SerialName("microphone_acknowledged")
    val microphoneAcknowledged: Boolean = false,
    @SerialName("camera_acknowledged")
    val cameraAcknowledged: Boolean = false,
    @SerialName("screen_capture_acknowledged")
    val screenCaptureAcknowledged: Boolean = false,
    @SerialName("notifications_acknowledged")
    val notificationsAcknowledged: Boolean = false,
    @SerialName("creator_content_types")
    val creatorContentTypes: List<String> = emptyList(),
    @SerialName("preferred_platforms")
    val preferredPlatforms: List<String> = emptyList(),
    @SerialName("experience_level")
    val experienceLevel: String? = null,
    @SerialName("primary_goal")
    val primaryGoal: String? = null,
)
