package com.universallive.app.cloud

data class CloudSession(val accessToken: String, val userId: String? = null, val email: String? = null)
data class CloudStreamStatus(val id: String, val status: String, val message: String? = null)

enum class CloudSyncState { SIGNED_OUT, READY, SYNCING, ERROR }

interface UniversalLiveCloudApi {
    suspend fun me(): Result<String>
    suspend fun activeStream(): Result<String?>
    suspend fun startStream(payloadJson: String): Result<String>
    suspend fun updateStreamStatus(id: String, status: String, message: String? = null): Result<Unit>
    suspend fun sendMetrics(id: String, payloadJson: String): Result<Unit>
    suspend fun stopStream(id: String): Result<Unit>
    suspend fun destinations(): Result<String>
    suspend fun saveDestination(payloadJson: String): Result<String>
    suspend fun scenes(): Result<String>
    suspend fun saveScene(payloadJson: String): Result<String>
    suspend fun settings(): Result<String?>
    suspend fun saveSettings(payloadJson: String): Result<Unit>
}
