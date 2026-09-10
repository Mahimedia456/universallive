package com.universallive.app.data.connections

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StreamingConnectionDto(
    val id: String,
    val platform: String,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("external_channel_id")
    val externalChannelId: String? = null,
    @SerialName("external_channel_name")
    val externalChannelName: String? = null,
    val status: String,
    @SerialName("is_default")
    val isDefault: Boolean = false,
    @SerialName("is_enabled")
    val isEnabled: Boolean = true,
)

@Serializable
data class CreateStreamingConnectionRequest(
    val platform: String,
    val displayName: String,
    val isDefault: Boolean = false,
)

@Serializable
data class PlatformChannelDto(
    val id: String,
    val platform: String,
    @SerialName("external_channel_id")
    val externalChannelId: String,
    @SerialName("channel_name")
    val channelName: String,
    @SerialName("channel_handle")
    val channelHandle: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("can_stream")
    val canStream: Boolean = false,
)

@Serializable
data class SaveRtmpCredentialRequest(
    val connectionId: String,
    val serverUrl: String,
    val streamKey: String,
)

@Serializable
data class RtmpCredentialStatusDto(
    val connectionId: String,
    val configured: Boolean,
    val lastRotatedAt: String? = null,
    val keyVersion: Int? = null,
)
