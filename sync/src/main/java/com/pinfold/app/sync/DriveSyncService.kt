package com.pinfold.app.sync

interface DriveSyncService {
    val isAvailable: Boolean
    val engine: String
    suspend fun authorize(): Result<String>
    suspend fun upload(name: String, bytes: ByteArray, token: String): Result<Unit>
    suspend fun download(name: String, token: String): Result<ByteArray>
    suspend fun revoke(token: String)
}

/**
 * Placeholder until the developer supplies the Android OAuth client id from the Google Cloud
 * console (section 19.12). No Google library is bundled, so the APK keeps zero Google dependencies.
 */
class UnavailableDriveSyncService : DriveSyncService {
    override val isAvailable: Boolean = false
    override val engine: String = "Solid — Drive sync is not configured"
    override suspend fun authorize(): Result<String> = Result.failure(IllegalStateException("Sync client id is not configured"))
    override suspend fun upload(name: String, bytes: ByteArray, token: String): Result<Unit> = Result.failure(IllegalStateException("Unavailable"))
    override suspend fun download(name: String, token: String): Result<ByteArray> = Result.failure(IllegalStateException("Unavailable"))
    override suspend fun revoke(token: String) = Unit
}
