package com.universallive.app.data.studio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SceneDto(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("aspect_ratio")
    val aspectRatio: String = "16:9",
    val width: Int = 1920,
    val height: Int = 1080,
    @SerialName("is_default")
    val isDefault: Boolean = false,
    @SerialName("sort_order")
    val sortOrder: Int = 0,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null,
    @SerialName("template_key")
    val templateKey: String? = null,
)

@Serializable
data class CreateSceneRequest(
    val name: String,
    val description: String? = null,
    val aspectRatio: String = "16:9",
    val width: Int = 1920,
    val height: Int = 1080,
    val isDefault: Boolean = false,
    val templateKey: String? = null,
)

@Serializable
data class SceneSourceDto(
    val id: String,
    @SerialName("scene_id")
    val sceneId: String,
    @SerialName("source_type")
    val sourceType: String,
    val name: String,
    @SerialName("z_index")
    val zIndex: Int = 0,
    @SerialName("is_visible")
    val isVisible: Boolean = true,
    @SerialName("is_locked")
    val isLocked: Boolean = false,
    val x: Double = 0.0,
    val y: Double = 0.0,
    val width: Double = 1.0,
    val height: Double = 1.0,
    val rotation: Double = 0.0,
    val opacity: Double = 1.0,
    val config: Map<String, String> = emptyMap(),
)

@Serializable
data class CreatorAssetDto(
    val id: String,
    @SerialName("asset_type")
    val assetType: String,
    val name: String,
    @SerialName("storage_bucket")
    val storageBucket: String,
    @SerialName("storage_path")
    val storagePath: String,
    @SerialName("public_url")
    val publicUrl: String? = null,
    @SerialName("mime_type")
    val mimeType: String? = null,
)

@Serializable
data class ScenePresetDto(
    val id: String,
    @SerialName("preset_key")
    val presetKey: String? = null,
    val name: String,
    val description: String? = null,
    val category: String? = null,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null,
)
