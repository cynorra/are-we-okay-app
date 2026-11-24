package com.cynorra.areweokay.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.net.URLEncoder
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

// ---------------------------------------------------------------------------------------------
// Models (mirror supabase/p1_migration.sql)
// ---------------------------------------------------------------------------------------------
@Serializable
data class PsychProfile(
    val user_id: String,
    val display_name: String,
    val title: String = "",
    val bio: String = "",
    val specialties: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val country: String = "",
    val city: String = "",
    val timezone: String = "UTC",
    val offers_online: Boolean = true,
    val offers_in_person: Boolean = false,
    val session_minutes: Int = 50,
    val price_amount: Double? = null,      // informational only - the app has no payments
    val price_currency: String = "USD",
    val verified_status: String = "pending"
) { val verified: Boolean get() = verified_status == "verified" }

@Serializable data class RatingStat(val psychologist_id: String, val avg_rating: Double = 0.0, val review_count: Int = 0)
@Serializable data class PsychReview(val id: String, val psychologist_id: String, val client_id: String, val appointment_id: String,
                                     val rating: Int, val comment: String? = null, val created_at: String)
@Serializable data class AvailRule(val id: String? = null, val psychologist_id: String, val weekday: Int, val start_min: Int, val end_min: Int)
@Serializable data class BusyRow(val start_at: String, val end_at: String)
@Serializable
data class Appointment(
    val id: String, val psychologist_id: String, val client_id: String,
    val start_at: String, val end_at: String, val mode: String = "online", val status: String,
    val proposed_start_at: String? = null, val client_note: String? = null, val room_name: String? = null,
    val late_cancel: Boolean = false, val price_amount: Double? = null, val price_currency: String? = null
)
@Serializable data class Conversation(val id: String, val client_id: String, val psychologist_id: String)
@Serializable data class ChatMessage(val id: Long, val conversation_id: String, val sender_id: String, val body: String, val created_at: String)
@Serializable
data class ClientRow(val client_id: String, val username: String, val avatar_emoji: String = "🌙", val appointments: Int = 0,
                     val next_start: String? = null, val conversation_id: String? = null, val grant_id: String? = null)
@Serializable
data class ShareGrant(val id: String, val user_id: String, val psychologist_id: String, val scopes: List<String> = emptyList(),
                      val created_at: String, val expires_at: String, val revoked_at: String? = null) {
    val active: Boolean get() = revoked_at == null && runCatching { Instant.now().isBefore(PsychRepo.parse(expires_at)) }.getOrDefault(false)
}
@Serializable data class AuditRow(val id: Long, val grant_id: String, val actor_id: String, val action: String, val created_at: String)
@Serializable data class PsychNote(val id: String, val client_id: String, val body: String, val created_at: String)
@Serializable private data class IdRow(val user_id: String)
@Serializable private data class PsychIdRow(val psychologist_id: String)
@Serializable private data class CheckinRow(val mood_level: Int? = null, val emotions: List<String> = emptyList(), val created_at: String)

data class DirectoryFilter(
    val query: String = "", val specialty: String? = null, val language: String? = null, val country: String? = null,
    val maxPrice: Double? = null, val online: Boolean = false, val inPerson: Boolean = false
)

/** Cross-screen deep link target (https://<invite_host>/p/<id> or areweokay://psychologist?id=<id>). */
object PsychDeepLink {
    var pendingId by androidx.compose.runtime.mutableStateOf<String?>(null)
}

// ---------------------------------------------------------------------------------------------
// Repository. Everything is protected by RLS / SECURITY DEFINER RPCs on the server; the client
// never decides authorization. All calls return null / false on failure (caller shows a message).
// ---------------------------------------------------------------------------------------------
object PsychRepo {
    private val json = SupabaseApi.json

    fun parse(iso: String): Instant = OffsetDateTime.parse(iso).toInstant()
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun safeQ(s: String) = s.replace(Regex("[,()*%\\\\:{}\"']"), " ").trim()
    private inline fun <T> attempt(block: () -> T): T? = try { block() } catch (e: Exception) { null }
    private inline fun <reified T> list(raw: String): List<T> = json.decodeFromString(ListSerializer(kotlinx.serialization.serializer<T>()), raw)

    // ---- profile / onboarding / verification ----
    @kotlinx.serialization.Serializable
    data class SocialLinks(
        val instagram: String? = null,
        val tiktok: String? = null,
        val x_handle: String? = null,
        val website: String? = null
    )

    /** Public social handles of a user (columns from supabase/p2_social_handles.sql). */
    suspend fun socialLinks(uid: String): SocialLinks? = attempt {
        list<SocialLinks>(SupabaseApi.get("/users?id=eq.$uid&select=instagram,tiktok,x_handle,website")).firstOrNull()
    }

    suspend fun myProfile(uid: String): PsychProfile? = attempt { list<PsychProfile>(SupabaseApi.get("/psychologist_profiles?user_id=eq.$uid")).firstOrNull() }

    suspend fun saveProfile(p: PsychProfile, exists: Boolean): Boolean = attempt {
        // verified_status is deliberately NOT sent: the server trigger owns it.
        val body = buildJsonObject {
            put("display_name", p.display_name); put("title", p.title); put("bio", p.bio)
            putJsonArray("specialties") { p.specialties.forEach { add(JsonPrimitive(it)) } }
            putJsonArray("languages") { p.languages.forEach { add(JsonPrimitive(it)) } }
            put("country", p.country); put("city", p.city); put("timezone", p.timezone)
            put("offers_online", p.offers_online); put("offers_in_person", p.offers_in_person)
            put("session_minutes", p.session_minutes)
            if (p.price_amount != null) put("price_amount", p.price_amount) else put("price_amount", JsonNull)
            put("price_currency", p.price_currency)
            if (!exists) put("user_id", p.user_id)
        }.toString()
        if (exists) SupabaseApi.patch("/psychologist_profiles?user_id=eq.${p.user_id}", body)
        else SupabaseApi.post("/psychologist_profiles", body)
        true
    } ?: false

    suspend fun uploadVerificationDoc(uid: String, fileName: String, bytes: ByteArray, mime: String, docType: String): Boolean = attempt {
        val safe = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").takeLast(60)
        val path = "$uid/${UUID.randomUUID()}_$safe"
        SupabaseApi.uploadObject("verification-docs", path, bytes, mime)
        SupabaseApi.post("/verification_documents", buildJsonObject { put("user_id", uid); put("storage_path", path); put("doc_type", docType) }.toString())
        true
    } ?: false

    suspend fun docCount(uid: String): Int = attempt { list<JsonObject>(SupabaseApi.get("/verification_documents?user_id=eq.$uid&select=id")).size } ?: 0

    // ---- directory ----
    suspend fun directory(f: DirectoryFilter): List<PsychProfile>? = attempt {
        val q = StringBuilder("/psychologist_profiles?verified_status=eq.verified&order=display_name.asc&limit=100")
        if (f.query.isNotBlank()) {
            val s = enc("*" + safeQ(f.query) + "*")
            q.append("&or=(display_name.ilike.$s,title.ilike.$s,bio.ilike.$s,city.ilike.$s)")
        }
        f.specialty?.let { q.append("&specialties=cs.${enc("{$it}")}") }
        f.language?.let { q.append("&languages=cs.${enc("{$it}")}") }
        f.country?.let { q.append("&country=eq.${enc(it)}") }
        f.maxPrice?.let { q.append("&price_amount=lte.$it") }
        if (f.online) q.append("&offers_online=eq.true")
        if (f.inPerson) q.append("&offers_in_person=eq.true")
        list<PsychProfile>(SupabaseApi.get(q.toString()))
    }

    suspend fun profile(id: String): PsychProfile? = attempt { list<PsychProfile>(SupabaseApi.get("/psychologist_profiles?user_id=eq.$id&verified_status=eq.verified")).firstOrNull() }
    suspend fun profilesByIds(ids: Collection<String>): Map<String, PsychProfile> = attempt {
        if (ids.isEmpty()) emptyMap() else
            list<PsychProfile>(SupabaseApi.get("/psychologist_profiles?user_id=in.(${ids.joinToString(",")})")).associateBy { it.user_id }
    } ?: emptyMap()

    suspend fun ratingStats(): Map<String, RatingStat> = attempt { list<RatingStat>(SupabaseApi.get("/psychologist_rating_stats")).associateBy { it.psychologist_id } } ?: emptyMap()
    suspend fun verifiedIds(): Set<String> = attempt { list<IdRow>(SupabaseApi.get("/psychologist_profiles?verified_status=eq.verified&select=user_id")).map { it.user_id }.toSet() } ?: emptySet()

    // ---- favorites / follows ----
    suspend fun favorites(uid: String): Set<String> = attempt { list<PsychIdRow>(SupabaseApi.get("/psychologist_favorites?client_id=eq.$uid&select=psychologist_id")).map { it.psychologist_id }.toSet() } ?: emptySet()
    suspend fun setFavorite(uid: String, pid: String, on: Boolean): Boolean = attempt {
        if (on) SupabaseApi.post("/psychologist_favorites", buildJsonObject { put("client_id", uid); put("psychologist_id", pid) }.toString())
        else SupabaseApi.delete("/psychologist_favorites?client_id=eq.$uid&psychologist_id=eq.$pid"); true
    } ?: false
    suspend fun follows(uid: String): Set<String> = attempt { list<PsychIdRow>(SupabaseApi.get("/psychologist_follows?follower_id=eq.$uid&select=psychologist_id")).map { it.psychologist_id }.toSet() } ?: emptySet()
    suspend fun setFollow(uid: String, pid: String, on: Boolean): Boolean = attempt {
        if (on) SupabaseApi.post("/psychologist_follows", buildJsonObject { put("follower_id", uid); put("psychologist_id", pid) }.toString())
        else SupabaseApi.delete("/psychologist_follows?follower_id=eq.$uid&psychologist_id=eq.$pid"); true
    } ?: false
    suspend fun followerCount(pid: String): Int = attempt { SupabaseApi.rpc("follower_count", """{"p":"$pid"}""").trim().toInt() } ?: 0

    // ---- reviews (server allows insert only after a completed session) ----
    suspend fun reviews(pid: String): List<PsychReview> = attempt { list<PsychReview>(SupabaseApi.get("/psychologist_reviews?psychologist_id=eq.$pid&order=created_at.desc&limit=50")) } ?: emptyList()
    /** A completed appointment of mine with this psychologist that has no review yet, or null. */
    suspend fun reviewableAppointment(uid: String, pid: String): String? = attempt {
        val done = list<Appointment>(SupabaseApi.get("/appointments?client_id=eq.$uid&psychologist_id=eq.$pid&status=eq.completed"))
        val reviewed = list<PsychReview>(SupabaseApi.get("/psychologist_reviews?client_id=eq.$uid&psychologist_id=eq.$pid")).map { it.appointment_id }.toSet()
        done.firstOrNull { it.id !in reviewed }?.id
    }
    suspend fun submitReview(uid: String, pid: String, apptId: String, rating: Int, comment: String): Boolean = attempt {
        SupabaseApi.post("/psychologist_reviews", buildJsonObject {
            put("client_id", uid); put("psychologist_id", pid); put("appointment_id", apptId); put("rating", rating)
            if (comment.isNotBlank()) put("comment", comment.take(1000))
        }.toString()); true
    } ?: false

    // ---- availability & appointments ----
    suspend fun availability(pid: String): List<AvailRule> = attempt { list<AvailRule>(SupabaseApi.get("/availability_rules?psychologist_id=eq.$pid")) } ?: emptyList()
    suspend fun saveAvailability(pid: String, rules: List<AvailRule>): Boolean = attempt {
        SupabaseApi.delete("/availability_rules?psychologist_id=eq.$pid")
        if (rules.isNotEmpty()) SupabaseApi.post("/availability_rules", buildJsonArray {
            rules.forEach { r -> add(buildJsonObject { put("psychologist_id", pid); put("weekday", r.weekday); put("start_min", r.start_min); put("end_min", r.end_min) }) }
        }.toString()); true
    } ?: false
    suspend fun busy(pid: String, from: Instant, to: Instant): List<Pair<Instant, Instant>> = attempt {
        list<BusyRow>(SupabaseApi.rpc("psychologist_busy", buildJsonObject { put("p_psych", pid); put("p_from", from.toString()); put("p_to", to.toString()) }.toString()))
            .map { parse(it.start_at) to parse(it.end_at) }
    } ?: emptyList()

    suspend fun requestAppointment(uid: String, p: PsychProfile, start: Instant, mode: String, note: String): Boolean = attempt {
        SupabaseApi.post("/appointments", buildJsonObject {
            put("client_id", uid); put("psychologist_id", p.user_id); put("start_at", start.toString())
            put("end_at", start.plusSeconds(p.session_minutes * 60L).toString()); put("mode", mode)
            if (note.isNotBlank()) put("client_note", note.take(500))
        }.toString()); true
    } ?: false
    suspend fun appointments(uid: String): List<Appointment> = attempt { list<Appointment>(SupabaseApi.get("/appointments?or=(client_id.eq.$uid,psychologist_id.eq.$uid)&order=start_at.desc&limit=100")) } ?: emptyList()
    /** action: confirm | decline | propose | accept_proposal | cancel | complete */
    suspend fun appointmentAction(id: String, action: String, newStart: Instant? = null): Boolean = attempt {
        SupabaseApi.rpc("appointment_action", buildJsonObject {
            put("p_id", id); put("p_action", action); if (newStart != null) put("p_new_start", newStart.toString())
        }.toString()); true
    } ?: false
    /** Room name only inside the join window (server enforced). */
    suspend fun joinRoom(apptId: String): String? = attempt {
        val raw = SupabaseApi.rpc("get_join_room", """{"p_id":"$apptId"}""").trim()
        if (raw == "null" || raw.isEmpty()) null else raw.trim('"')
    }

    // ---- messaging ----
    suspend fun conversation(clientId: String, pid: String): Conversation? = attempt {
        list<Conversation>(SupabaseApi.get("/conversations?client_id=eq.$clientId&psychologist_id=eq.$pid")).firstOrNull()
            ?: list<Conversation>(SupabaseApi.post("/conversations", buildJsonObject { put("client_id", clientId); put("psychologist_id", pid) }.toString())).firstOrNull()
    }
    suspend fun myConversations(uid: String): List<Conversation> = attempt { list<Conversation>(SupabaseApi.get("/conversations?or=(client_id.eq.$uid,psychologist_id.eq.$uid)")) } ?: emptyList()
    suspend fun messages(convId: String, afterId: Long = 0): List<ChatMessage>? = attempt {
        list<ChatMessage>(SupabaseApi.get("/messages?conversation_id=eq.$convId&id=gt.$afterId&order=id.asc&limit=200"))
    }
    suspend fun sendMessage(convId: String, uid: String, body: String): Boolean = attempt {
        SupabaseApi.post("/messages", buildJsonObject { put("conversation_id", convId); put("sender_id", uid); put("body", body.take(4000)) }.toString()); true
    } ?: false

    /**
     * Supabase Realtime (Phoenix channel over WebSocket) for new rows in `messages` of one conversation.
     * RLS applies to realtime: only the two parties receive events. The UI also polls as a fallback.
     */
    fun subscribeMessages(convId: String, onInsert: (ChatMessage) -> Unit): () -> Unit {
        val topic = "realtime:msg-$convId"
        var ref = 1
        var hb: Thread? = null
        val ws: WebSocket = SupabaseApi.client.newWebSocket(Request.Builder().url(SupabaseApi.realtimeUrl).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val join = buildJsonObject {
                    put("topic", topic); put("event", "phx_join"); put("ref", (ref++).toString())
                    put("payload", buildJsonObject {
                        put("config", buildJsonObject {
                            putJsonArray("postgres_changes") {
                                add(buildJsonObject { put("event", "INSERT"); put("schema", "public"); put("table", "messages"); put("filter", "conversation_id=eq.$convId") })
                            }
                        })
                        put("access_token", SupabaseApi.currentAccessToken())
                    })
                }
                webSocket.send(join.toString())
                hb = Thread {
                    try { while (true) { Thread.sleep(25_000); webSocket.send("""{"topic":"phoenix","event":"heartbeat","payload":{},"ref":"${ref++}"}""") } } catch (_: Exception) {}
                }.also { it.isDaemon = true; it.start() }
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val o = json.parseToJsonElement(text).jsonObject
                    if (o["event"]?.jsonPrimitive?.contentOrNull != "postgres_changes") return
                    val rec = o["payload"]?.jsonObject?.get("data")?.jsonObject?.get("record") ?: return
                    onInsert(json.decodeFromJsonElement(ChatMessage.serializer(), rec))
                } catch (_: Exception) { }
            }
        })
        return { hb?.interrupt(); ws.close(1000, null) }
    }

    // ---- psychologist side ----
    suspend fun myClients(): List<ClientRow> = attempt { list<ClientRow>(SupabaseApi.rpc("my_clients")) } ?: emptyList()
    suspend fun notes(clientId: String): List<PsychNote> = attempt { list<PsychNote>(SupabaseApi.get("/psychologist_notes?client_id=eq.$clientId&order=created_at.desc")) } ?: emptyList()
    suspend fun addNote(pid: String, clientId: String, body: String): Boolean = attempt {
        SupabaseApi.post("/psychologist_notes", buildJsonObject { put("psychologist_id", pid); put("client_id", clientId); put("body", body.take(8000)) }.toString()); true
    } ?: false
    suspend fun publishPost(uid: String, kind: String, content: String, tags: List<String>): Boolean = attempt {
        SupabaseApi.post("/posts", buildJsonObject {
            put("user_id", uid); put("content", content.take(2000)); put("is_anonymous", false); put("kind", kind)
            putJsonArray("tags") { tags.forEach { add(JsonPrimitive(it)) } }
        }.toString()); true
    } ?: false

    // ---- share with my psychologist (consent, time-boxed, revocable, audited server-side) ----
    suspend fun grants(uid: String): List<ShareGrant> = attempt { list<ShareGrant>(SupabaseApi.get("/share_grants?user_id=eq.$uid&order=created_at.desc&limit=30")) } ?: emptyList()
    suspend fun createGrant(uid: String, pid: String, scopes: List<String>, days: Int): ShareGrant? = attempt {
        list<ShareGrant>(SupabaseApi.post("/share_grants", buildJsonObject {
            put("user_id", uid); put("psychologist_id", pid)
            putJsonArray("scopes") { scopes.forEach { add(JsonPrimitive(it)) } }
            put("expires_at", Instant.now().plusSeconds(days * 86_400L - 60).toString())
        }.toString())).firstOrNull()
    }
    suspend fun revokeGrant(id: String): Boolean = attempt { SupabaseApi.rpc("revoke_share", """{"p_grant":"$id"}"""); true } ?: false
    suspend fun audit(grantId: String): List<AuditRow> = attempt { list<AuditRow>(SupabaseApi.get("/share_audit_log?grant_id=eq.$grantId&order=id.desc&limit=50")) } ?: emptyList()

    /** Builds and uploads the snapshot: last 30 days of mood (+emotions) and the journal excerpts the user ticked. */
    suspend fun pushSnapshot(uid: String, grant: ShareGrant, journal: List<JournalEntry>): Boolean = attempt {
        val since = Instant.now().minusSeconds(30 * 86_400L).toString()
        val rows = list<CheckinRow>(SupabaseApi.get("/checkins?user_id=eq.$uid&created_at=gte.${enc(since)}&select=mood_level,emotions,created_at&order=created_at.desc&limit=120"))
        val withEmotions = "emotions" in grant.scopes
        val payload = buildJsonObject {
            put("generated_at", Instant.now().toString())
            put("note", "User-shared self-reflection data. Not a clinical measurement.")
            putJsonArray("moods") {
                rows.forEach { r -> add(buildJsonObject {
                    put("at", r.created_at); put("level", r.mood_level ?: 0)
                    if (withEmotions) putJsonArray("emotions") { r.emotions.forEach { add(JsonPrimitive(it)) } }
                }) }
            }
            if ("journal" in grant.scopes) putJsonArray("journal") {
                journal.forEach { j -> add(buildJsonObject { put("at", Instant.ofEpochMilli(j.created_at).toString()); put("template", j.template); put("body", j.body.take(3000)) }) }
            }
        }
        SupabaseApi.rpc("upsert_share_snapshot", buildJsonObject { put("p_grant", grant.id); put("p_payload", payload) }.toString()); true
    } ?: false
    suspend fun readSnapshot(grantId: String): JsonElement? = attempt {
        val raw = SupabaseApi.rpc("read_share_snapshot", """{"p_grant":"$grantId"}""")
        json.parseToJsonElement(raw)
    }
}
