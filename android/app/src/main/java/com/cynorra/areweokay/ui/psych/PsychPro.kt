@file:OptIn(ExperimentalLayoutApi::class)

package com.cynorra.areweokay.ui.psych

import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cynorra.areweokay.R
import com.cynorra.areweokay.data.AvailRule
import com.cynorra.areweokay.data.LocalDataRepository
import com.cynorra.areweokay.data.PsychNote
import com.cynorra.areweokay.data.PsychProfile
import com.cynorra.areweokay.data.PsychRepo
import com.cynorra.areweokay.theme.OkBlack
import com.cynorra.areweokay.theme.OkMuted
import com.cynorra.areweokay.theme.OkOrangeShade
import com.cynorra.areweokay.theme.OkTeal
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import java.util.TimeZone

@Composable
private fun myUid(): String? = LocalDataRepository.current.currentUser.collectAsState().value?.id

// ------------------------------------------------------------------------------------------
// Psychologist onboarding (profile + licence/diploma upload). Verification is manual (admin).
// ------------------------------------------------------------------------------------------
@Composable
internal fun OnboardingScreen(onSaved: () -> Unit) {
    val uid = myUid() ?: return
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var existing by remember { mutableStateOf<PsychProfile?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var specs by remember { mutableStateOf(setOf<String>()) }
    var langs by remember { mutableStateOf(setOf<String>()) }
    var country by remember { mutableStateOf(Locale.getDefault().country) }
    var city by remember { mutableStateOf("") }
    var tz by remember { mutableStateOf(TimeZone.getDefault().id) }
    var online by remember { mutableStateOf(true) }
    var inPerson by remember { mutableStateOf(false) }
    var minutes by remember { mutableStateOf("50") }
    var price by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("USD") }
    var ack by remember { mutableStateOf(false) }
    var docs by remember { mutableIntStateOf(0) }
    var msg by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        PsychRepo.myProfile(uid)?.let { p ->
            existing = p; name = p.display_name; title = p.title; bio = p.bio; specs = p.specialties.toSet(); langs = p.languages.toSet()
            country = p.country; city = p.city; tz = p.timezone; online = p.offers_online; inPerson = p.offers_in_person
            minutes = p.session_minutes.toString(); price = p.price_amount?.toString() ?: ""; currency = p.price_currency
        }
        docs = PsychRepo.docCount(uid); loaded = true
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            try {
                val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                val mime = ctx.contentResolver.getType(uri) ?: "application/octet-stream"
                if (bytes == null || bytes.size > 8 * 1024 * 1024 || !(mime.startsWith("image/") || mime == "application/pdf")) {
                    msg = ctx.getString(R.string.psy_doc_bad)
                } else if (PsychRepo.uploadVerificationDoc(uid, uri.lastPathSegment ?: "doc", bytes, mime, "license")) {
                    docs++; msg = ctx.getString(R.string.psy_doc_ok)
                } else msg = ctx.getString(R.string.psy_error_generic)
            } catch (e: Exception) { msg = ctx.getString(R.string.psy_error_generic) }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        H(stringResource(R.string.psy_onboard_title))
        Muted(stringResource(R.string.psy_onboard_intro))
        if (!loaded) { Muted(stringResource(R.string.psy_loading)); return@Column }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(name, { name = it.take(80) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.psy_f_name)) })
        OutlinedTextField(title, { title = it.take(80) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.psy_f_title)) })
        OutlinedTextField(bio, { bio = it.take(2000) }, Modifier.fillMaxWidth(), minLines = 3, label = { Text(stringResource(R.string.psy_f_bio)) })
        Muted(stringResource(R.string.psy_f_specs))
        FlowRow { SPECIALTIES.forEach { (k, r) -> Pill(stringResource(r), k in specs) { specs = if (k in specs) specs - k else specs + k } } }
        Muted(stringResource(R.string.psy_f_langs))
        FlowRow { DIR_LANGS.forEach { l -> Pill(langName(l), l in langs) { langs = if (l in langs) langs - l else langs + l } } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(country, { country = it.uppercase().take(2) }, Modifier.weight(1f), singleLine = true, label = { Text(stringResource(R.string.psy_f_country)) })
            OutlinedTextField(city, { city = it.take(60) }, Modifier.weight(2f), singleLine = true, label = { Text(stringResource(R.string.psy_f_city)) })
        }
        OutlinedTextField(tz, { tz = it.trim().take(50) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.psy_f_tz)) })
        Row(verticalAlignment = Alignment.CenterVertically) { Switch(online, { online = it }); Text("  " + stringResource(R.string.psy_online)) }
        Row(verticalAlignment = Alignment.CenterVertically) { Switch(inPerson, { inPerson = it }); Text("  " + stringResource(R.string.psy_in_person)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit).take(3) }, Modifier.weight(1f), singleLine = true, label = { Text(stringResource(R.string.psy_f_minutes)) })
            OutlinedTextField(price, { price = it.filter { c -> c.isDigit() || c == '.' }.take(8) }, Modifier.weight(1f), singleLine = true, label = { Text(stringResource(R.string.psy_f_price)) })
            OutlinedTextField(currency, { currency = it.uppercase().take(3) }, Modifier.weight(1f), singleLine = true, label = { Text(stringResource(R.string.psy_f_currency)) })
        }
        Muted(stringResource(R.string.psy_price_info))
        Spacer(Modifier.height(8.dp))
        Panel {
            Text(stringResource(R.string.psy_doc_title), fontWeight = FontWeight.Bold)
            Muted(stringResource(R.string.psy_doc_desc))
            Muted(stringResource(R.string.psy_doc_count, docs))
            Btn(stringResource(R.string.psy_doc_upload), { picker.launch("*/*") }, primary = false)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(ack, { ack = it })
            Text(stringResource(R.string.psy_pro_ack), fontSize = 12.sp, color = OkBlack, modifier = Modifier.weight(1f))
        }
        msg?.let { Text(it, color = OkOrangeShade, fontSize = 13.sp) }
        val tzOk = runCatching { ZoneId.of(tz); true }.getOrDefault(false)
        if (!tzOk) Text(stringResource(R.string.psy_tz_bad), color = OkOrangeShade, fontSize = 12.sp)
        Btn(stringResource(R.string.psy_save_submit), {
            busy = true
            scope.launch {
                val p = PsychProfile(
                    user_id = uid, display_name = name.trim(), title = title.trim(), bio = bio.trim(), specialties = specs.toList(),
                    languages = langs.toList(), country = country, city = city.trim(), timezone = tz, offers_online = online,
                    offers_in_person = inPerson, session_minutes = (minutes.toIntOrNull() ?: 50).coerceIn(15, 180),
                    price_amount = price.toDoubleOrNull(), price_currency = currency.ifBlank { "USD" }
                )
                val ok = PsychRepo.saveProfile(p, existing != null)
                busy = false
                if (ok) onSaved() else msg = ctx.getString(R.string.psy_error_generic)
            }
        }, Modifier.fillMaxWidth(), enabled = ack && name.trim().length >= 2 && title.isNotBlank() && tzOk && (online || inPerson) && !busy)
    }
}

// ------------------------------------------------------------------------------------------
// Psychologist dashboard
// ------------------------------------------------------------------------------------------
@Composable
internal fun DashboardScreen(go: (PS) -> Unit) {
    val uid = myUid() ?: return
    var prof by remember { mutableStateOf<PsychProfile?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var followers by remember { mutableIntStateOf(0) }
    var showLink by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { prof = PsychRepo.myProfile(uid); followers = PsychRepo.followerCount(uid); loaded = true }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        H(stringResource(R.string.psy_panel))
        val p = prof
        if (!loaded) { Muted(stringResource(R.string.psy_loading)); return@Column }
        if (p == null) {
            Panel {
                Text(stringResource(R.string.psy_join_title), fontWeight = FontWeight.Bold)
                Muted(stringResource(R.string.psy_onboard_intro))
                Spacer(Modifier.height(6.dp))
                Btn(stringResource(R.string.psy_join_cta), { go(PS.Onboard) }, Modifier.fillMaxWidth())
            }
            return@Column
        }
        Panel {
            Text(p.display_name, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            when (p.verified_status) {
                "verified" -> VerifiedBadge()
                "rejected" -> Text(stringResource(R.string.psy_vs_rejected), color = OkOrangeShade, fontSize = 13.sp)
                else -> Text(stringResource(R.string.psy_vs_pending), color = OkMuted, fontSize = 13.sp)
            }
            Muted(stringResource(R.string.psy_followers, followers))
        }
        Btn(stringResource(R.string.psy_edit_profile), { go(PS.Onboard) }, Modifier.fillMaxWidth(), primary = false)
        if (!p.verified) { Muted(stringResource(R.string.psy_hidden_until_verified), Modifier.padding(top = 8.dp)); return@Column }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(stringResource(R.string.psy_my_appts), { go(PS.Appts) }, Modifier.weight(1f))
            Btn(stringResource(R.string.psy_clients), { go(PS.Clients) }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(stringResource(R.string.psy_availability), { go(PS.Avail) }, Modifier.weight(1f), primary = false)
            Btn(stringResource(R.string.psy_messages), { go(PS.Messages) }, Modifier.weight(1f), primary = false)
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(stringResource(R.string.psy_post_title), { go(PS.Compose) }, Modifier.weight(1f), primary = false)
            Btn(stringResource(R.string.psy_share_profile), { showLink = true }, Modifier.weight(1f), primary = false)
        }
    }
    if (showLink) prof?.let { ProfileLinkDialog(it) { showLink = false } }
}

// ------------------------------------------------------------------------------------------
// Weekly availability (stored in the psychologist's own time zone)
// ------------------------------------------------------------------------------------------
@Composable
internal fun AvailabilityScreen() {
    val uid = myUid() ?: return
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val rules = remember { mutableStateListOf<AvailRule>() }
    var tz by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { tz = PsychRepo.myProfile(uid)?.timezone ?: ""; rules.clear(); rules.addAll(PsychRepo.availability(uid)) }

    fun add(day: Int) {
        TimePickerDialog(ctx, { _, h1, m1 ->
            TimePickerDialog(ctx, { _, h2, m2 ->
                val s = h1 * 60 + m1; val e = h2 * 60 + m2
                if (e > s) rules.add(AvailRule(null, uid, day, s, e)) else msg = ctx.getString(R.string.psy_avail_bad)
            }, (h1 + 4).coerceAtMost(23), 0, true).apply { setTitle(ctx.getString(R.string.psy_avail_end)) }.show()
        }, 9, 0, true).apply { setTitle(ctx.getString(R.string.psy_avail_start)) }.show()
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        H(stringResource(R.string.psy_availability))
        Muted(stringResource(R.string.psy_avail_tz, tz))
        (1..7).forEach { d ->
            Panel {
                Text(DayOfWeek.of(d).getDisplayName(TextStyle.FULL, Locale.getDefault()), fontWeight = FontWeight.Bold, color = OkBlack)
                FlowRow {
                    rules.filter { it.weekday == d }.sortedBy { it.start_min }.forEach { r ->
                        Pill(minToHm(r.start_min) + "–" + minToHm(r.end_min) + "  ✕", true) { rules.remove(r) }
                    }
                    Pill("+ " + stringResource(R.string.psy_avail_add), false) { add(d) }
                }
            }
        }
        msg?.let { Text(it, color = OkTeal, fontSize = 13.sp) }
        Spacer(Modifier.height(8.dp))
        Btn(stringResource(R.string.psy_save), {
            scope.launch { msg = ctx.getString(if (PsychRepo.saveAvailability(uid, rules.toList())) R.string.psy_saved else R.string.psy_error_generic) }
        }, Modifier.fillMaxWidth())
    }
}

// ------------------------------------------------------------------------------------------
// Clients, shared data (consent-based, audited on the server) and private session notes
// ------------------------------------------------------------------------------------------
@Composable
internal fun ClientsScreen(go: (PS) -> Unit) {
    var rows by remember { mutableStateOf<List<com.cynorra.areweokay.data.ClientRow>?>(null) }
    LaunchedEffect(Unit) { rows = PsychRepo.myClients() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        H(stringResource(R.string.psy_clients))
        val r = rows
        if (r == null) Muted(stringResource(R.string.psy_loading)) else if (r.isEmpty()) Muted(stringResource(R.string.psy_no_clients))
        r?.forEach { c ->
            Panel(Modifier.clickableRow { go(PS.Client(c.client_id, c.username, c.conversation_id, c.grant_id)) }) {
                Text(c.avatar_emoji + " " + c.username, fontWeight = FontWeight.Bold, color = OkBlack)
                Muted(stringResource(R.string.psy_sessions_n, c.appointments) + (c.next_start?.let { " · " + stringResource(R.string.psy_next) + ": " + fmtTime(it) } ?: ""))
                if (c.grant_id != null) Tag(stringResource(R.string.psy_shared_data))
            }
        }
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier = this.then(Modifier.clickable(onClick = onClick))

@Composable
internal fun ClientDetailScreen(c: PS.Client, go: (PS) -> Unit) {
    val uid = myUid() ?: return
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var notes by remember { mutableStateOf<List<PsychNote>>(emptyList()) }
    var text by remember { mutableStateOf("") }
    var snap by remember { mutableStateOf<JsonElement?>(null) }
    var snapErr by remember { mutableStateOf(false) }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(tick) { notes = PsychRepo.notes(c.clientId) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        H(c.name)
        Btn(stringResource(R.string.psy_send_message), {
            scope.launch {
                val conv = c.convId?.let { id -> com.cynorra.areweokay.data.Conversation(id, c.clientId, uid) } ?: PsychRepo.conversation(c.clientId, uid)
                if (conv != null) go(PS.Chat(conv.id, c.name, c.clientId))
            }
        }, Modifier.fillMaxWidth(), primary = false)
        Spacer(Modifier.height(10.dp))
        H(stringResource(R.string.psy_shared_data))
        if (c.grantId == null) Muted(stringResource(R.string.psy_no_shared))
        else {
            Muted(stringResource(R.string.psy_shared_note))
            Btn(stringResource(R.string.psy_view_shared), { scope.launch { val s = PsychRepo.readSnapshot(c.grantId); snap = s; snapErr = s == null } }, primary = false)
            if (snapErr) Text(stringResource(R.string.psy_share_expired), color = OkOrangeShade, fontSize = 13.sp)
            SnapshotView(snap)
        }
        Spacer(Modifier.height(10.dp))
        H(stringResource(R.string.psy_notes))
        Muted(stringResource(R.string.psy_notes_private))
        OutlinedTextField(text, { text = it.take(8000) }, Modifier.fillMaxWidth(), minLines = 3, label = { Text(stringResource(R.string.psy_note_new)) })
        Btn(stringResource(R.string.psy_save), {
            scope.launch { if (PsychRepo.addNote(uid, c.clientId, text.trim())) { text = ""; tick++ } else snapErr = false }
        }, enabled = text.isNotBlank())
        notes.forEach { n -> Panel { Muted(fmtTime(n.created_at)); Text(n.body, fontSize = 14.sp, color = OkBlack) } }
    }
}

@Composable
private fun SnapshotView(snap: JsonElement?) {
    if (snap == null) return
    val o = runCatching { snap.jsonObject }.getOrNull() ?: return
    val moods = o["moods"]?.jsonArray.orEmpty()
    val levels = moods.mapNotNull { it.jsonObject["level"]?.jsonPrimitive?.intOrNull }.filter { it > 0 }
    Panel {
        Text(stringResource(R.string.psy_mood_avg, if (levels.isEmpty()) 0.0 else levels.average(), levels.size), fontWeight = FontWeight.Bold)
        moods.take(30).forEach { m ->
            val mo = m.jsonObject
            val lvl = mo["level"]?.jsonPrimitive?.intOrNull ?: 0
            val emo = mo["emotions"]?.jsonArray?.joinToString(", ") { it.jsonPrimitive.contentOrNull ?: "" }.orEmpty()
            Text(fmtTime(mo["at"]?.jsonPrimitive?.contentOrNull ?: "") + "  ·  " + lvl + "/5" + if (emo.isNotBlank()) "  ·  $emo" else "", fontSize = 12.sp, color = OkBlack)
        }
        val j = o["journal"]?.jsonArray.orEmpty()
        if (j.isNotEmpty()) {
            Spacer(Modifier.height(8.dp)); Text(stringResource(R.string.psy_scope_journal), fontWeight = FontWeight.Bold)
            j.forEach { e -> Text(e.jsonObject["body"]?.jsonPrimitive?.contentOrNull ?: "", fontSize = 13.sp, color = OkBlack, modifier = Modifier.padding(vertical = 4.dp)) }
        }
    }
}

private fun kotlinx.serialization.json.JsonArray?.orEmpty() = this ?: kotlinx.serialization.json.JsonArray(emptyList())

// ------------------------------------------------------------------------------------------
// Psychologist content: short tips / articles posted to the community feed with a verified badge
// ------------------------------------------------------------------------------------------
@Composable
internal fun ComposePostScreen() {
    val uid = myUid() ?: return
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var kind by remember { mutableStateOf("tip") }
    var body by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var ack by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        H(stringResource(R.string.psy_post_title))
        Muted(stringResource(R.string.psy_post_intro))
        FlowRow {
            Pill(stringResource(R.string.psy_kind_tip), kind == "tip") { kind = "tip" }
            Pill(stringResource(R.string.psy_kind_article), kind == "article") { kind = "article" }
            Pill(stringResource(R.string.psy_kind_post), kind == "post") { kind = "post" }
        }
        OutlinedTextField(body, { body = it.take(2000) }, Modifier.fillMaxWidth(), minLines = 5, label = { Text(stringResource(R.string.psy_post_body)) })
        OutlinedTextField(tags, { tags = it.take(100) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.psy_post_tags)) })
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(ack, { ack = it })
            Text(stringResource(R.string.psy_post_ack), fontSize = 12.sp, color = OkBlack, modifier = Modifier.weight(1f))
        }
        msg?.let { Text(it, color = OkTeal, fontSize = 13.sp) }
        Btn(stringResource(R.string.psy_publish), {
            scope.launch {
                val t = tags.split(",", " ").map { it.trim().trimStart('#').lowercase() }.filter { it.isNotBlank() }.take(5)
                val ok = PsychRepo.publishPost(uid, kind, body.trim(), t)
                msg = ctx.getString(if (ok) R.string.psy_published else R.string.psy_error_generic)
                if (ok) body = ""
            }
        }, Modifier.fillMaxWidth(), enabled = ack && body.trim().length >= 20)
    }
}
