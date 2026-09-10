package com.universallive.app.data.notifications

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationDto(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val severity: String = "info",
    @SerialName("action_type")
    val actionType: String? = null,
    @SerialName("read_at")
    val readAt: String? = null,
    @SerialName("created_at")
    val createdAt: String,
)
