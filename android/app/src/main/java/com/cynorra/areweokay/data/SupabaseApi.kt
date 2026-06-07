package com.cynorra.areweokay.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

@Serializable
data class SupabaseUserResponse(
    val username: String,
    val avatar_emoji: String = "🌙"
)

@Serializable
data class AuthUserResponse(
    val id: String
)

@Serializable
data class SupabaseReactionResponse(
    val type: String,
    val user_id: String
)

@Serializable
data class SupabasePostResponse(
    val id: String,
    val user_id: String,
    val checkin_id: String? = null,
    val content: String,
    val mood: MoodState? = null,
    val is_anonymous: Boolean = true,
    val created_at: String,
    val users: SupabaseUserResponse? = null,
    val reactions: List<SupabaseReactionResponse> = emptyList()
)

object SupabaseApi {
    private const val BASE_URL = "https://pmtqntkipmamthoiddtl.supabase.co/rest/v1"
    private const val API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBtdHFudGtpcG1hbXRob2lkZHRsIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc3NzM4NTIzOSwiZXhwIjoyMDkyOTYxMjM5fQ._h8st17YkDP3-7o2vhocSASKXppOO_EpgsaR2U0DBd8"

    val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun createAuthUser(email: String): String = withContext(Dispatchers.IO) {
        val authUrl = "https://pmtqntkipmamthoiddtl.supabase.co/auth/v1/admin/users"
        val bodyJson = """{"email":"$email","password":"okayness123!","email_confirm":true}"""
        val requestBody = bodyJson.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(authUrl)
            .post(requestBody)
            .addHeader("apikey", API_KEY)
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected HTTP code $response")
            response.body?.string() ?: ""
        }
    }

    suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$BASE_URL$path")
            .addHeader("apikey", API_KEY)
            .addHeader("Authorization", "Bearer $API_KEY")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected HTTP code $response")
            response.body?.string() ?: ""
        }
    }

    suspend fun post(path: String, bodyJson: String): String = withContext(Dispatchers.IO) {
        val requestBody = bodyJson.toRequestBody(mediaType)
        val request = Request.Builder()
            .url("$BASE_URL$path")
            .post(requestBody)
            .addHeader("apikey", API_KEY)
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected HTTP code $response")
            response.body?.string() ?: ""
        }
    }

    suspend fun patch(path: String, bodyJson: String): String = withContext(Dispatchers.IO) {
        val requestBody = bodyJson.toRequestBody(mediaType)
        val request = Request.Builder()
            .url("$BASE_URL$path")
            .patch(requestBody)
            .addHeader("apikey", API_KEY)
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected HTTP code $response")
            response.body?.string() ?: ""
        }
    }

    suspend fun delete(path: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$BASE_URL$path")
            .delete()
            .addHeader("apikey", API_KEY)
            .addHeader("Authorization", "Bearer $API_KEY")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected HTTP code $response")
            response.body?.string() ?: ""
        }
    }

    suspend fun signInWithPassword(email: String, password: String): String = withContext(Dispatchers.IO) {
        val authUrl = "https://pmtqntkipmamthoiddtl.supabase.co/auth/v1/token?grant_type=password"
        val bodyJson = """{"email":"$email","password":"$password"}"""
        val requestBody = bodyJson.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(authUrl)
            .post(requestBody)
            .addHeader("apikey", API_KEY)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Geçersiz e-posta veya şifre.")
            response.body?.string() ?: ""
        }
    }
}

