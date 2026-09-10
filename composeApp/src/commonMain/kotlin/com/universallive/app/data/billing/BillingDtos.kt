package com.universallive.app.data.billing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlanEntitlementsDto(
    @SerialName("max_simultaneous_destinations")
    val maxSimultaneousDestinations: Int = 1,
    @SerialName("max_resolution")
    val maxResolution: String = "720p",
    @SerialName("advanced_scenes")
    val advancedScenes: Boolean = false,
    @SerialName("advanced_overlays")
    val advancedOverlays: Boolean = false,
    @SerialName("advanced_analytics")
    val advancedAnalytics: Boolean = false,
)

@Serializable
data class PlanDto(
    val id: String,
    @SerialName("plan_key")
    val planKey: String,
    val name: String,
    val description: String? = null,
    val entitlements: PlanEntitlementsDto = PlanEntitlementsDto(),
)

@Serializable
data class UserEntitlementDto(
    @SerialName("plan_key")
    val planKey: String = "free",
    val status: String = "active",
    val source: String = "system",
    @SerialName("expires_at")
    val expiresAt: String? = null,
    @SerialName("effective_entitlements")
    val effectiveEntitlements: PlanEntitlementsDto? = null,
)

@Serializable
data class VerifyPurchaseRequest(
    val store: String,
    val productId: String,
    val transactionId: String? = null,
    val originalTransactionId: String? = null,
    val purchaseToken: String? = null,
    val planKey: String? = null,
)
