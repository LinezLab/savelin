package com.pinfold.app.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object DriveRest {
    private val client = OkHttpClient()
    private const val BASE = "https://www.googleapis.com/drive/v3"

    suspend fun listAppdata(token: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$BASE/files?spaces=appDataFolder&fields=files(id,name)&pageSize=1000")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(
                    IllegalStateException("Drive returned HTTP ${response.code}"),
                )
                val body = response.body?.string().orEmpty()
                Result.success(Regex("\"id\"").findAll(body).count())
            }
        }.getOrElse { Result.failure(it) }
    }

    suspend fun uploadAppdata(token: String, name: String, bytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val metadata = "{\"name\":\"$name\",\"parents\":[\"appDataFolder\"]}"
                .toRequestBody("application/json; charset=UTF-8".toMediaType())
            val media = bytes.toRequestBody("application/octet-stream".toMediaType())
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addPart(MultipartBody.Part.createFormData("metadata", null, metadata))
                .addPart(MultipartBody.Part.createFormData("media", null, media))
                .build()
            val request = Request.Builder()
                .url("$BASE/files?uploadType=multipart&fields=id")
                .header("Authorization", "Bearer $token")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Result.failure<Unit>(IllegalStateException("Drive upload returned HTTP ${response.code}"))
                } else Result.success(Unit)
            }
        }.getOrElse { Result.failure(it) }
    }
}
