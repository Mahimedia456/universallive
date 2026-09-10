package com.universallive.app.data.support

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupportTicketDto(
    val id: String,
    val category: String,
    val subject: String,
    val description: String,
    val status: String,
    val priority: String,
    @SerialName("created_at")
    val createdAt: String,
)

@Serializable
data class CreateSupportTicketRequest(
    val category: String,
    val subject: String,
    val description: String,
    val diagnostics: Map<String, String> = emptyMap(),
)

@Serializable
data class SupportMessageDto(
    val id: String,
    @SerialName("ticket_id")
    val ticketId: String,
    @SerialName("sender_type")
    val senderType: String,
    val message: String,
    @SerialName("created_at")
    val createdAt: String,
)
