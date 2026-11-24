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
data class SupabaseReactionResponse(
    val type: String,
    val user_id: String
)

@Serializable
data class SupabaseCommentResponse(
    val id: String,
    val user_id: String,
    val content: String,
    val created_at: String,
    val users: SupabaseUserResponse? = null
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
    val reactions: List<SupabaseReactionResponse> = emptyList(),
    val comments: List<SupabaseCommentResponse> = emptyList()
)

@Serializable
data class BlockRow(val blocked_id: String)

@Serializable
data class AuthSessionUser(
    val id: String,
    val email: String? = null
)

@Serializable
data class AuthSessionResponse(
    val access_token: String,
    val refresh_token: String,
    val expires_in: Long = 3600,
    val user: AuthSessionUser
)

object SupabaseApi {
    private const val SUPABASE_URL = "https://jnkpeuttglghtegrvabg.supabase.co"
    private const val REST_BASE_URL = "$SUPABASE_URL/rest/v1"
    private const val AUTH_BASE_URL = "$SUPABASE_URL/auth/v1"

    // Public/anon key: safe to ship in the client. It grants no access on its own -
    // every table is locked down with Row Level Security policies, and per-user access
    // is only unlocked once a real user access token is set via setSession().
    private const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Impua3BldXR0Z2xnaHRlZ3J2YWJnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTU0NDk5MDcsImV4cCI6MjExMTAyNTkwN30.Jt1Lde-ZTDGtbRBUapFvxBKzFVHYiVYFHWEMe8Aheio"

    @Volatile
    private var accessToken: String? = null

    fun setSession(accessToken: String?) {
        this.accessToken = accessToken
    }

    private fun authBearer(): String = accessToken ?: ANON_KEY

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

    suspend fun signInWithIdToken(idToken: String): String = withContext(Dispatchers.IO) {
        val bodyJson = """{"provider":"google","id_token":"$idToken"}"""
        val requestBody = bodyJson.toRequestBody(mediaType)
        val request = Request.Builder()
            .url("$AUTH_BASE_URL/token?grant_type=id_token")
            .post(requestBody)
            .addHeader("apikey", ANON_KEY)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Google sign-in failed (${response.code}).")
            response.body?.string() ?: ""
        }
    }

    suspend fun refreshSession(refreshToken: String): String = withContext(Dispatchers.IO) {
        val bodyJson = """{"refresh_token":"$refreshToken"}"""
        val requestBody = bodyJson.toRequestBody(mediaType)
        val request = Request.Builder()
            .url("$AUTH_BASE_URL/token?grant_type=refresh_token")
            .post(requestBody)
            .addHeader("apikey", ANON_KEY)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Oturum yenilenemedi.")
            response.body?.string() ?: ""
        }
    }

    suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$REST_BASE_URL$path")
            .addHeader("apikey", ANON_KEY)
            .addHeader("Authorization", "Bearer ${authBearer()}")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected HTTP code $response")
            response.body?.string() ?: ""
        }
    }

    suspend fun post(path: String, bodyJson: String): String = withContext(Dispatchers.IO) {
        val requestBody = bodyJson.toRequestBody(mediaType)
        val request = Request.Builder()
            .url("$REST_BASE_URL$path")
            .post(requestBody)
            .addHeader("apikey", ANON_KEY)
            .addHeader("Authorization", "Bearer ${authBearer()}")
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
            .url("$REST_BASE_URL$path")
            .patch(requestBody)
            .addHeader("apikey", ANON_KEY)
            .addHeader("Authorization", "Bearer ${authBearer()}")
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected HTTP code $response")
            response.body?.string() ?: ""
        }
    }

    suspend fun delete(path: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$REST_BASE_URL$path")
            .delete()
            .addHeader("apikey", ANON_KEY)
            .addHeader("Authorization", "Bearer ${authBearer()}")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected HTTP code $response")
            response.body?.string() ?: ""
        }
    }

    // ---- P1 helpers (psychologist platform) ----
    val realtimeUrl: String get() = "wss://${SUPABASE_URL.removePrefix("https://")}/realtime/v1/websocket?apikey=$ANON_KEY&vsn=1.0.0"
    fun currentAccessToken(): String = authBearer()

    /** POST /rest/v1/rpc/<fn>; returns the raw JSON body. */
    suspend fun rpc(fn: String, bodyJson: String = "{}"): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$REST_BASE_URL/rpc/$fn")
            .post(bodyJson.toRequestBody(mediaType))
            .addHeader("apikey", ANON_KEY)
            .addHeader("Authorization", "Bearer ${authBearer()}")
            .addHeader("Content-Type", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("RPC $fn failed (${response.code})")
            response.body?.string() ?: ""
        }
    }

    /** Uploads bytes to a PRIVATE storage bucket (policies restrict the folder to the user's own id). */
    suspend fun uploadObject(bucket: String, path: String, bytes: ByteArray, contentType: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$SUPABASE_URL/storage/v1/object/$bucket/$path")
            .post(bytes.toRequestBody(contentType.toMediaType()))
            .addHeader("apikey", ANON_KEY)
            .addHeader("Authorization", "Bearer ${authBearer()}")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Upload failed (${response.code})")
        }
    }
}
