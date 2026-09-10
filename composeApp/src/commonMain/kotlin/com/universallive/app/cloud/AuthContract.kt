package com.universallive.app.cloud

interface UniversalLiveAuth {
    val session: CloudSession?
    suspend fun signUp(email: String, password: String): Result<CloudSession?>
    suspend fun signIn(email: String, password: String): Result<CloudSession>
    suspend fun restore(): Result<CloudSession?>
    suspend fun signOut(): Result<Unit>
}
