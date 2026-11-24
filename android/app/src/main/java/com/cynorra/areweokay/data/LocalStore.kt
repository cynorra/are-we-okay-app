package com.cynorra.areweokay.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

@Serializable
data class JournalEntry(
    val id: String,
    val template: String,
    val body: String,
    val mood_level: Int? = null,
    val created_at: Long
)

data class Badge(val key: String, val emoji: String, val earned: Boolean)

/**
 * Device-local data: consent record, journal (AES-GCM, Android Keystore key, never leaves the
 * device and is excluded from backup), reminder prefs, counters for badges, pending invite.
 */
class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("AreWeOkayLocal", Context.MODE_PRIVATE)
    private val json = SupabaseApi.json

    // ---- consent ----
    var consentVersion: Int
        get() = prefs.getInt("consent_version", 0)
        set(v) { prefs.edit().putInt("consent_version", v).apply() }
    val hasValidConsent: Boolean get() = consentVersion >= CURRENT_CONSENT_VERSION

    var pendingMood: String?
        get() = prefs.getString("pending_mood", null)
        set(v) { prefs.edit().putString("pending_mood", v).apply() }

    // ---- reminder ----
    var reminderEnabled: Boolean
        get() = prefs.getBoolean("reminder_on", true)
        set(v) { prefs.edit().putBoolean("reminder_on", v).apply() }
    var reminderHour: Int
        get() = prefs.getInt("reminder_hour", 20)
        set(v) { prefs.edit().putInt("reminder_hour", v).apply() }

    var lastCheckinDay: String?
        get() = prefs.getString("last_checkin_day", null)
        set(v) { prefs.edit().putString("last_checkin_day", v).apply() }

    // ---- counters ----
    var breathingSessions: Int
        get() = prefs.getInt("breath_n", 0)
        set(v) { prefs.edit().putInt("breath_n", v).apply() }

    // ---- invite ----
    var pendingInviterId: String?
        get() = prefs.getString("pending_ref", null)
        set(v) { prefs.edit().putString("pending_ref", v).apply() }

    // ---- journal ----
    private val _journal = MutableStateFlow(loadJournal())
    val journal: StateFlow<List<JournalEntry>> = _journal.asStateFlow()

    fun addJournal(template: String, body: String, moodLevel: Int?): JournalEntry {
        val e = JournalEntry(UUID.randomUUID().toString(), template, body, moodLevel, System.currentTimeMillis())
        persist(listOf(e) + _journal.value)
        return e
    }

    fun deleteJournal(id: String) = persist(_journal.value.filterNot { it.id == id })

    private fun persist(list: List<JournalEntry>) {
        _journal.value = list
        try {
            prefs.edit().putString("journal_enc", encrypt(json.encodeToString(list))).apply()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun loadJournal(): List<JournalEntry> = try {
        prefs.getString("journal_enc", null)?.let { json.decodeFromString<List<JournalEntry>>(decrypt(it)) } ?: emptyList()
    } catch (e: Exception) { emptyList() }

    fun badges(streak: Int, checkinCount: Int): List<Badge> = listOf(
        Badge("first_checkin", "🌱", checkinCount >= 1),
        Badge("streak_3", "🔥", streak >= 3),
        Badge("streak_7", "🌟", streak >= 7),
        Badge("streak_14", "🏅", streak >= 14),
        Badge("streak_30", "🏆", streak >= 30),
        Badge("journal_1", "📓", _journal.value.isNotEmpty()),
        Badge("journal_10", "📚", _journal.value.size >= 10),
        Badge("breath_1", "🌬️", breathingSessions >= 1)
    )

    /** Wipes everything stored by this class, including the Keystore key (account deletion). */
    fun wipeAll() {
        prefs.edit().clear().apply()
        _journal.value = emptyList()
        try {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(KEY_ALIAS)
        } catch (_: Exception) {}
    }

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    private fun encrypt(plain: String): String {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key())
        return Base64.encodeToString(c.iv + c.doFinal(plain.toByteArray()), Base64.NO_WRAP)
    }

    private fun decrypt(b64: String): String {
        val raw = Base64.decode(b64, Base64.NO_WRAP)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, raw, 0, 12))
        return String(c.doFinal(raw, 12, raw.size - 12))
    }

    companion object {
        fun today(): String = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        const val CURRENT_CONSENT_VERSION = 1
        private const val KEY_ALIAS = "areweokay_journal_key"
    }
}
