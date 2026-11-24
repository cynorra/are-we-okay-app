@file:OptIn(ExperimentalLayoutApi::class)

package com.cynorra.areweokay.ui.psych

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import com.cynorra.areweokay.R
import com.cynorra.areweokay.components.NoAds
import com.cynorra.areweokay.data.Appointment
import com.cynorra.areweokay.data.ChatMessage
import com.cynorra.areweokay.data.ClientRow
import com.cynorra.areweokay.data.DirectoryFilter
import com.cynorra.areweokay.data.LocalDataRepository
import com.cynorra.areweokay.data.PsychDeepLink
import com.cynorra.areweokay.data.PsychProfile
import com.cynorra.areweokay.data.PsychRepo
import com.cynorra.areweokay.data.ShareGrant
import com.cynorra.areweokay.notify.AppointmentReminders
import com.cynorra.areweokay.theme.OkBeige
import com.cynorra.areweokay.theme.OkBlack
import com.cynorra.areweokay.theme.OkBorder
import com.cynorra.areweokay.theme.OkMuted
import com.cynorra.areweokay.theme.OkOrange
import com.cynorra.areweokay.theme.OkOrangeLight
import com.cynorra.areweokay.theme.OkOrangeShade
import com.cynorra.areweokay.theme.OkSurface
import com.cynorra.areweokay.theme.OkTeal
import com.cynorra.areweokay.theme.OkTealLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale
import java.util.Calendar

/**
 * Hub for the psychologist platform (directory, booking, messaging, sharing, psychologist panel).
 * Hides the banner ad (sensitive health context) and always offers the crisis button.
 */
@Composable
fun PsychHub(onClose: () -> Unit, onCrisis: () -> Unit) {
    NoAds()
    val stack = remember { mutableStateListOf<PS>(PS.Directory) }
    LaunchedEffect(PsychDeepLink.pendingId) {
        PsychDeepLink.pendingId?.let { stack.add(PS.Profile(it)); PsychDeepLink.pendingId = null }
    }
    val back: () -> Unit = { if (stack.size > 1) stack.removeAt(stack.lastIndex) else onClose() }
    val go: (PS) -> Unit = { stack.add(it) }
    BackHandler { back() }

    Column(Modifier.fillMaxSize().background(OkBeige)) {
        Row(
            Modifier.fillMaxWidth().background(OkSurface).border(1.dp, OkBorder).padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { back() }) { Text("←  " + stringResource(R.string.psy_back), color = OkBlack, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onCrisis) { Text("🆘 " + stringResource(R.string.help_button), color = OkOrangeShade, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        }
        // Always-visible legal framing.
        Text(
            stringResource(R.string.psy_disclaimer), fontSize = 11.sp, color = OkMuted, lineHeight = 15.sp,
            modifier = Modifier.fillMaxWidth().background(OkTealLight).padding(horizontal = 14.dp, vertical = 6.dp)
        )
        Box(Modifier.weight(1f)) {
            when (val s = stack.last()) {
                PS.Directory -> DirectoryScreen(go)
                is PS.Profile -> ProfileScreen(s.id, go)
                is PS.Book -> BookingScreen(s.id, onDone = { stack.removeAt(stack.lastIndex); go(PS.Appts) })
                PS.Appts -> AppointmentsScreen(go)
                PS.Messages -> MessagesScreen(go)
                is PS.Chat -> ChatScreen(s.convId, s.title, s.otherId)
                PS.Onboard -> OnboardingScreen(onSaved = { stack.removeAt(stack.lastIndex) })
                PS.Dashboard -> DashboardScreen(go)
                PS.Clients -> ClientsScreen(go)
                is PS.Client -> ClientDetailScreen(s, go)
                PS.Avail -> AvailabilityScreen()
                PS.Compose -> ComposePostScreen()
                PS.Share -> ShareWithPsychologistScreen()
            }
        }
    }
}

@Composable
private fun myId(): String? = LocalDataRepository.current.currentUser.collectAsState().value?.id

@Composable
private fun priceText(p: PsychProfile): String =
    if (p.price_amount == null) stringResource(R.string.psy_price_ask)
    else stringResource(R.string.psy_price_fmt, "%.0f".format(p.price_amount), p.price_currency, p.session_minutes)

// ------------------------------------------------------------------------------------------
// Directory (seekers)
// ------------------------------------------------------------------------------------------
@Composable
private fun DirectoryScreen(go: (PS) -> Unit) {
    val uid = myId()
    val scope = rememberCoroutineScope()
    var q by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(DirectoryFilter()) }
    var maxPrice by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<PsychProfile>?>(null) }
    var failed by remember { mutableStateOf(false) }
    var langs by remember { mutableStateOf(setOf<String>()) }
    var countries by remember { mutableStateOf(setOf<String>()) }
    var favs by remember { mutableStateOf(setOf<String>()) }
    var onlyFavs by remember { mutableStateOf(false) }
    var stats by remember { mutableStateOf(mapOf<String, com.cynorra.areweokay.data.RatingStat>()) }

    LaunchedEffect(uid) { if (uid != null) favs = PsychRepo.favorites(uid); stats = PsychRepo.ratingStats() }
    LaunchedEffect(q, maxPrice) { delay(350); filter = filter.copy(query = q, maxPrice = maxPrice.toDoubleOrNull()) }
    LaunchedEffect(filter) {
        results = null
        val r = PsychRepo.directory(filter)
        failed = r == null
        results = r
        if (r != null) { langs = langs + r.flatMap { it.languages }; countries = countries + r.map { it.country }.filter { it.isNotBlank() } }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        H(stringResource(R.string.psy_dir_title))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(stringResource(R.string.psy_my_appts), { go(PS.Appts) }, Modifier.weight(1f), primary = false)
            Btn(stringResource(R.string.psy_messages), { go(PS.Messages) }, Modifier.weight(1f), primary = false)
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(stringResource(R.string.psy_share_title), { go(PS.Share) }, Modifier.weight(1f), primary = false)
            Btn(stringResource(R.string.psy_panel), { go(PS.Dashboard) }, Modifier.weight(1f), primary = false)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = q, onValueChange = { q = it.take(60) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.psy_search_hint)) }
        )
        Spacer(Modifier.height(6.dp))
        Muted(stringResource(R.string.psy_filter_spec))
        FlowRow {
            Pill(stringResource(R.string.psy_all), filter.specialty == null) { filter = filter.copy(specialty = null) }
            SPECIALTIES.forEach { (k, r) -> Pill(stringResource(r), filter.specialty == k) { filter = filter.copy(specialty = if (filter.specialty == k) null else k) } }
        }
        Muted(stringResource(R.string.psy_filter_mode))
        FlowRow {
            Pill(stringResource(R.string.psy_online), filter.online) { filter = filter.copy(online = !filter.online) }
            Pill(stringResource(R.string.psy_in_person), filter.inPerson) { filter = filter.copy(inPerson = !filter.inPerson) }
            Pill("♥ " + stringResource(R.string.psy_favorites), onlyFavs) { onlyFavs = !onlyFavs }
        }
        if (langs.isNotEmpty()) {
            Muted(stringResource(R.string.psy_filter_lang))
            FlowRow {
                Pill(stringResource(R.string.psy_all), filter.language == null) { filter = filter.copy(language = null) }
                langs.sorted().forEach { l -> Pill(langName(l), filter.language == l) { filter = filter.copy(language = if (filter.language == l) null else l) } }
            }
        }
        if (countries.isNotEmpty()) {
            Muted(stringResource(R.string.psy_filter_country))
            FlowRow {
                Pill(stringResource(R.string.psy_all), filter.country == null) { filter = filter.copy(country = null) }
                countries.sorted().forEach { c -> Pill(countryName(c), filter.country == c) { filter = filter.copy(country = if (filter.country == c) null else c) } }
            }
        }
        OutlinedTextField(
            value = maxPrice, onValueChange = { maxPrice = it.filter { c -> c.isDigit() || c == '.' }.take(7) }, singleLine = true,
            label = { Text(stringResource(R.string.psy_filter_price)) }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))

        val list = results?.filter { !onlyFavs || it.user_id in favs }
        when {
            failed -> Muted(stringResource(R.string.psy_error_load))
            list == null -> Muted(stringResource(R.string.psy_loading))
            list.isEmpty() -> Muted(stringResource(R.string.psy_dir_empty))
            else -> list.forEach { p ->
                Panel(Modifier.clickable { go(PS.Profile(p.user_id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.display_name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = OkBlack)
                            Text(p.title, fontSize = 13.sp, color = OkMuted)
                            VerifiedBadge()
                        }
                        val isFav = p.user_id in favs
                        TextButton(onClick = {
                            if (uid != null) scope.launch {
                                if (PsychRepo.setFavorite(uid, p.user_id, !isFav)) favs = if (isFav) favs - p.user_id else favs + p.user_id
                            }
                        }) { Text(if (isFav) "♥" else "♡", fontSize = 22.sp, color = OkOrange) }
                    }
                    FlowRow(Modifier.padding(top = 6.dp)) {
                        p.specialties.take(4).forEach { k -> specRes(k)?.let { Tag(stringResource(it)) } }
                    }
                    val st = stats[p.user_id]
                    Muted(
                        listOfNotNull(
                            st?.let { "★ %.1f (%d)".format(it.avg_rating, it.review_count) },
                            priceText(p),
                            listOf(p.city, countryName(p.country)).filter { it.isNotBlank() }.joinToString(", ").ifBlank { null },
                            p.languages.joinToString(" / ") { langName(it) }.ifBlank { null }
                        ).joinToString("  ·  ")
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------
// Profile
// ------------------------------------------------------------------------------------------
@Composable
private fun ProfileScreen(id: String, go: (PS) -> Unit) {
    val uid = myId()
    val ctx = LocalContext.current
    val repo = LocalDataRepository.current
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<PsychProfile?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var stat by remember { mutableStateOf<com.cynorra.areweokay.data.RatingStat?>(null) }
    var reviews by remember { mutableStateOf(listOf<com.cynorra.areweokay.data.PsychReview>()) }
    var fav by remember { mutableStateOf(false) }
    var follow by remember { mutableStateOf(false) }
    var followers by remember { mutableStateOf(0) }
    var reviewAppt by remember { mutableStateOf<String?>(null) }
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    var showLink by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    var social by remember { mutableStateOf<PsychRepo.SocialLinks?>(null) }

    LaunchedEffect(id, tick) {
        profile = PsychRepo.profile(id); loaded = true
        social = PsychRepo.socialLinks(id)
        stat = PsychRepo.ratingStats()[id]
        reviews = PsychRepo.reviews(id)
        followers = PsychRepo.followerCount(id)
        if (uid != null) {
            fav = id in PsychRepo.favorites(uid); follow = id in PsychRepo.follows(uid)
            reviewAppt = PsychRepo.reviewableAppointment(uid, id)
        }
    }
    val p = profile
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (p == null) { Muted(stringResource(if (loaded) R.string.psy_not_found else R.string.psy_loading)); return@Column }
        Text(p.display_name, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = OkBlack)
        Text(p.title, color = OkMuted)
        VerifiedBadge()
        Spacer(Modifier.height(8.dp))
        stat?.let { Text("★ %.1f  (%d)".format(it.avg_rating, it.review_count), fontWeight = FontWeight.Bold, color = OkBlack) }
        Muted(stringResource(R.string.psy_followers, followers))
        Spacer(Modifier.height(6.dp))
        Panel {
            Text(p.bio.ifBlank { "-" }, fontSize = 14.sp, color = OkBlack, lineHeight = 20.sp)
            FlowRow(Modifier.padding(top = 8.dp)) { p.specialties.forEach { k -> specRes(k)?.let { Tag(stringResource(it)) } } }
            Muted(p.languages.joinToString(" / ") { langName(it) })
            Muted(listOf(p.city, countryName(p.country)).filter { it.isNotBlank() }.joinToString(", "))
            Muted(listOfNotNull(if (p.offers_online) stringResource(R.string.psy_online) else null, if (p.offers_in_person) stringResource(R.string.psy_in_person) else null).joinToString(" · "))
            Spacer(Modifier.height(6.dp))
            Text(priceText(p), fontWeight = FontWeight.Bold, color = OkBlack)
            Muted(stringResource(R.string.psy_price_info))
        }
        social?.let { sl ->
            val links = listOfNotNull(
                sl.instagram?.takeIf { it.isNotBlank() }?.let { "Instagram" to socialUrl("https://instagram.com/", it) },
                sl.tiktok?.takeIf { it.isNotBlank() }?.let { "TikTok" to socialUrl("https://tiktok.com/@", it) },
                sl.x_handle?.takeIf { it.isNotBlank() }?.let { "X" to socialUrl("https://x.com/", it) },
                sl.website?.takeIf { it.isNotBlank() }?.let { "Web" to socialUrl("https://", it) }
            )
            if (links.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    links.forEach { (label, url) ->
                        Btn(label, {
                            runCatching { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
                        }, primary = false)
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        if (uid != null && uid != p.user_id) {
            Btn(stringResource(R.string.psy_book), { go(PS.Book(p.user_id)) }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(stringResource(R.string.psy_send_message), {
                    scope.launch {
                        val c = PsychRepo.conversation(uid, p.user_id)
                        if (c != null) go(PS.Chat(c.id, p.display_name, p.user_id)) else msg = ctx.getString(R.string.psy_error_generic)
                    }
                }, Modifier.weight(1f), primary = false)
                Btn((if (fav) "♥ " else "♡ ") + stringResource(R.string.psy_favorite), {
                    scope.launch { if (PsychRepo.setFavorite(uid, p.user_id, !fav)) fav = !fav }
                }, Modifier.weight(1f), primary = false)
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(stringResource(if (follow) R.string.psy_unfollow else R.string.psy_follow), {
                    scope.launch { if (PsychRepo.setFollow(uid, p.user_id, !follow)) { follow = !follow; tick++ } }
                }, Modifier.weight(1f), primary = false)
                Btn(stringResource(R.string.psy_share_profile), { showLink = true }, Modifier.weight(1f), primary = false)
            }
            TextButton(onClick = {
                scope.launch { msg = ctx.getString(if (repo.reportContent("psychologist", p.user_id, "other")) R.string.psy_reported else R.string.psy_error_generic) }
            }) { Text(stringResource(R.string.psy_report), color = OkMuted, fontSize = 12.sp) }
        } else {
            Btn(stringResource(R.string.psy_share_profile), { showLink = true }, Modifier.fillMaxWidth(), primary = false)
        }
        msg?.let { Text(it, color = OkOrangeShade, fontSize = 13.sp) }

        Spacer(Modifier.height(10.dp))
        H(stringResource(R.string.psy_reviews))
        if (reviewAppt != null && uid != null) {
            Panel {
                Text(stringResource(R.string.psy_leave_review), fontWeight = FontWeight.Bold)
                Row { (1..5).forEach { i -> Text(if (i <= rating) "★" else "☆", fontSize = 30.sp, color = OkOrange, modifier = Modifier.clickable { rating = i }.padding(2.dp)) } }
                OutlinedTextField(comment, { comment = it.take(1000) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.psy_review_hint)) })
                Spacer(Modifier.height(6.dp))
                Btn(stringResource(R.string.psy_submit), {
                    scope.launch { if (PsychRepo.submitReview(uid, p.user_id, reviewAppt!!, rating, comment)) { comment = ""; tick++ } else msg = ctx.getString(R.string.psy_error_generic) }
                })
            }
        } else Muted(stringResource(R.string.psy_review_rule))
        if (reviews.isEmpty()) Muted(stringResource(R.string.psy_no_reviews))
        reviews.forEach { r ->
            Panel {
                Text("★".repeat(r.rating) + "☆".repeat(5 - r.rating), color = OkOrange)
                if (!r.comment.isNullOrBlank()) Text(r.comment, fontSize = 14.sp, color = OkBlack)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Muted(fmtTime(r.created_at))
                    TextButton(onClick = { scope.launch { repo.reportContent("review", r.id, "other") } }) { Text(stringResource(R.string.psy_report), fontSize = 11.sp, color = OkMuted) }
                }
            }
        }
    }
    if (showLink && p != null) ProfileLinkDialog(p) { showLink = false }
}

@Composable
fun ProfileLinkDialog(p: PsychProfile, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val link = "https://" + ctx.getString(R.string.invite_host) + "/p/" + p.user_id
    val bmp = remember(link) { qrBitmap(link).asImageBitmap() }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(p.display_name) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                androidx.compose.foundation.Image(bmp, null, Modifier.width(200.dp).height(200.dp))
                Spacer(Modifier.height(8.dp))
                Text(link, fontSize = 12.sp, color = OkMuted)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, p.display_name + "\n" + link) }
                ctx.startActivity(Intent.createChooser(send, null))
            }) { Text(stringResource(R.string.psy_share_profile)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
    )
}

// ------------------------------------------------------------------------------------------
// Booking (slots computed in the psychologist's time zone, shown in the device time zone)
// ------------------------------------------------------------------------------------------
fun computeSlots(p: PsychProfile, rules: List<com.cynorra.areweokay.data.AvailRule>, busy: List<Pair<Instant, Instant>>, now: Instant, days: Int = 14): List<Instant> {
    val zone = runCatching { ZoneId.of(p.timezone) }.getOrDefault(ZoneOffset.UTC)
    val today = now.atZone(zone).toLocalDate()
    val out = mutableListOf<Instant>()
    for (d in 0..days) {
        val date = today.plusDays(d.toLong())
        rules.filter { it.weekday == date.dayOfWeek.value }.sortedBy { it.start_min }.forEach { r ->
            var m = r.start_min
            while (m + p.session_minutes <= r.end_min) {
                val start = date.atTime(m / 60, m % 60).atZone(zone).toInstant()
                val end = start.plusSeconds(p.session_minutes * 60L)
                if (start >= now.plusSeconds(2 * 3600) && busy.none { (bs, be) -> start < be && end > bs }) out.add(start)
                m += p.session_minutes
            }
        }
    }
    return out
}

@Composable
private fun BookingScreen(id: String, onDone: () -> Unit) {
    val uid = myId()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var p by remember { mutableStateOf<PsychProfile?>(null) }
    var slots by remember { mutableStateOf<List<Instant>?>(null) }
    var chosen by remember { mutableStateOf<Instant?>(null) }
    var mode by remember { mutableStateOf("online") }
    var note by remember { mutableStateOf("") }
    var agree by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    val devZone = remember { ZoneId.systemDefault() }

    LaunchedEffect(id) {
        val prof = PsychRepo.profile(id); p = prof
        if (prof != null) {
            val now = Instant.now()
            val rules = PsychRepo.availability(id)
            val b = PsychRepo.busy(id, now, now.plusSeconds(16 * 86400L))
            slots = computeSlots(prof, rules, b, now)
            if (!prof.offers_online) mode = "in_person"
        }
    }
    val prof = p
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (prof == null) { Muted(stringResource(R.string.psy_loading)); return@Column }
        H(stringResource(R.string.psy_book) + ": " + prof.display_name)
        Muted(stringResource(R.string.psy_tz_note, devZone.id, prof.timezone))
        Spacer(Modifier.height(6.dp))
        val s = slots
        if (s == null) Muted(stringResource(R.string.psy_loading))
        else if (s.isEmpty()) Muted(stringResource(R.string.psy_no_slots))
        else s.groupBy { it.atZone(devZone).toLocalDate() }.forEach { (date, list) ->
            Text(date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) + " " + date.toString(), fontWeight = FontWeight.Bold, color = OkBlack, modifier = Modifier.padding(top = 6.dp))
            FlowRow { list.forEach { t -> Pill(t.atZone(devZone).toLocalTime().toString().take(5), chosen == t) { chosen = t } } }
        }
        Spacer(Modifier.height(8.dp))
        FlowRow {
            if (prof.offers_online) Pill(stringResource(R.string.psy_online), mode == "online") { mode = "online" }
            if (prof.offers_in_person) Pill(stringResource(R.string.psy_in_person), mode == "in_person") { mode = "in_person" }
        }
        OutlinedTextField(note, { note = it.take(500) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.psy_note_hint)) })
        Spacer(Modifier.height(6.dp))
        Panel {
            Text(priceText(prof), fontWeight = FontWeight.Bold)
            Muted(stringResource(R.string.psy_price_info))
            Muted(stringResource(R.string.psy_cancel_rules))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(agree, { agree = it })
            Text(stringResource(R.string.psy_booking_ack), fontSize = 12.sp, color = OkBlack, modifier = Modifier.weight(1f))
        }
        msg?.let { Text(it, color = OkOrangeShade, fontSize = 13.sp) }
        Btn(stringResource(R.string.psy_request), {
            val t = chosen
            if (uid != null && t != null) { busy = true; scope.launch {
                val ok = PsychRepo.requestAppointment(uid, prof, t, mode, note)
                busy = false
                if (ok) onDone() else msg = ctx.getString(R.string.psy_slot_taken)
            } }
        }, Modifier.fillMaxWidth(), enabled = chosen != null && agree && !busy)
    }
}

// ------------------------------------------------------------------------------------------
// Appointments (both roles)
// ------------------------------------------------------------------------------------------
@Composable
private fun AppointmentsScreen(go: (PS) -> Unit) {
    val uid = myId() ?: return
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<Appointment>?>(null) }
    var names by remember { mutableStateOf(mapOf<String, String>()) }
    var tick by remember { mutableIntStateOf(0) }
    var msg by remember { mutableStateOf<String?>(null) }
    var confirmCancel by remember { mutableStateOf<Appointment?>(null) }

    LaunchedEffect(tick) {
        val a = PsychRepo.appointments(uid)
        val clients = PsychRepo.myClients().associate { it.client_id to it.username }
        val profs = PsychRepo.profilesByIds(a.map { it.psychologist_id }.toSet()).mapValues { it.value.display_name }
        names = clients + profs
        items = a
        AppointmentReminders.sync(ctx, a)
    }
    fun act(a: Appointment, action: String, t: Instant? = null) = scope.launch {
        msg = if (PsychRepo.appointmentAction(a.id, action, t)) null else ctx.getString(R.string.psy_error_generic)
        tick++
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        H(stringResource(R.string.psy_my_appts))
        msg?.let { Text(it, color = OkOrangeShade, fontSize = 13.sp) }
        val list = items
        if (list == null) Muted(stringResource(R.string.psy_loading))
        else if (list.isEmpty()) Muted(stringResource(R.string.psy_no_appts))
        list?.forEach { a ->
            val iAmPsych = a.psychologist_id == uid
            val other = if (iAmPsych) a.client_id else a.psychologist_id
            val ended = runCatching { Instant.now().isAfter(PsychRepo.parse(a.end_at)) }.getOrDefault(false)
            Panel {
                Text(names[other] ?: stringResource(R.string.psy_client), fontWeight = FontWeight.Bold, color = OkBlack)
                Text(fmtTime(a.start_at), color = OkBlack)
                Muted(stringResource(R.string.psy_status_label) + ": " + statusLabel(a.status) + " · " + stringResource(if (a.mode == "online") R.string.psy_online else R.string.psy_in_person))
                if (a.status == "reschedule_proposed" && a.proposed_start_at != null) Text(stringResource(R.string.psy_proposed) + ": " + fmtTime(a.proposed_start_at), color = OkTeal, fontWeight = FontWeight.Bold)
                if (!a.client_note.isNullOrBlank() && iAmPsych) Muted("“${a.client_note}”")
                if (a.late_cancel) Muted(stringResource(R.string.psy_late_cancel))
                FlowRow(Modifier.padding(top = 6.dp)) {
                    if (iAmPsych && a.status == "requested") {
                        Pill(stringResource(R.string.psy_confirm), true) { act(a, "confirm") }
                        Pill(stringResource(R.string.psy_decline), false) { act(a, "decline") }
                        Pill(stringResource(R.string.psy_propose), false) { pickDateTime(ctx) { t -> act(a, "propose", t) } }
                    }
                    if (!iAmPsych && a.status == "reschedule_proposed") {
                        Pill(stringResource(R.string.psy_accept_proposal), true) { act(a, "accept_proposal") }
                    }
                    if (a.status == "confirmed" && a.mode == "online") {
                        Pill(stringResource(R.string.psy_join), true) {
                            scope.launch {
                                val room = PsychRepo.joinRoom(a.id)
                                if (room == null) msg = ctx.getString(R.string.psy_join_window)
                                else {
                                    // Jitsi room with an unguessable name. SECURITY NOTE: the public meet.jit.si server is NOT
                                    // adequate for special-category health data (HIPAA/KVKK/GDPR). Point R.string.video_base_url
                                    // at a self-hosted Jitsi or swap in a BAA-covered provider (Daily/Twilio) before real use.
                                    val base = ctx.getString(R.string.video_base_url).trimEnd('/')
                                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("$base/$room#config.prejoinPageEnabled=true")))
                                }
                            }
                        }
                    }
                    if (iAmPsych && a.status == "confirmed" && ended) Pill(stringResource(R.string.psy_complete), true) { act(a, "complete") }
                    if (a.status in listOf("requested", "confirmed", "reschedule_proposed") && !ended) Pill(stringResource(R.string.psy_cancel_appt), false) { confirmCancel = a }
                    Pill(stringResource(R.string.psy_messages), false) {
                        scope.launch {
                            val c = PsychRepo.conversation(a.client_id, a.psychologist_id)
                            if (c != null) go(PS.Chat(c.id, names[other] ?: "", other)) else msg = ctx.getString(R.string.psy_error_generic)
                        }
                    }
                }
            }
        }
    }
    confirmCancel?.let { a ->
        val late = a.status == "confirmed" && runCatching { Duration.between(Instant.now(), PsychRepo.parse(a.start_at)).toHours() < 24 }.getOrDefault(false)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmCancel = null },
            title = { Text(stringResource(R.string.psy_cancel_appt)) },
            text = { Text(stringResource(if (late) R.string.psy_cancel_late_warn else R.string.psy_cancel_rules)) },
            confirmButton = { TextButton(onClick = { act(a, "cancel"); confirmCancel = null }) { Text(stringResource(R.string.psy_cancel_appt), color = OkOrangeShade) } },
            dismissButton = { TextButton(onClick = { confirmCancel = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun statusLabel(s: String): String = stringResource(
    when (s) {
        "requested" -> R.string.psy_st_requested; "confirmed" -> R.string.psy_st_confirmed; "declined" -> R.string.psy_st_declined
        "reschedule_proposed" -> R.string.psy_st_proposed; "cancelled" -> R.string.psy_st_cancelled; else -> R.string.psy_st_completed
    }
)

private fun pickDateTime(ctx: android.content.Context, onPicked: (Instant) -> Unit) {
    val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    DatePickerDialog(ctx, { _, y, m, d ->
        TimePickerDialog(ctx, { _, h, min ->
            val t = LocalDate.of(y, m + 1, d).atTime(h, min).atZone(ZoneId.systemDefault()).toInstant()
            onPicked(t)
        }, 10, 0, true).show()
    }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
}

// ------------------------------------------------------------------------------------------
// Messaging
// ------------------------------------------------------------------------------------------
@Composable
private fun MessagesScreen(go: (PS) -> Unit) {
    val uid = myId() ?: return
    var rows by remember { mutableStateOf<List<Triple<String, String, String>>?>(null) } // convId, title, otherId
    LaunchedEffect(Unit) {
        val convs = PsychRepo.myConversations(uid)
        val clients = PsychRepo.myClients().associate { it.client_id to it.username }
        val profs = PsychRepo.profilesByIds(convs.map { it.psychologist_id }.toSet())
        rows = convs.map { c ->
            val other = if (c.client_id == uid) c.psychologist_id else c.client_id
            Triple(c.id, if (c.client_id == uid) profs[other]?.display_name ?: "?" else clients[other] ?: "?", other)
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        H(stringResource(R.string.psy_messages))
        Muted(stringResource(R.string.psy_chat_privacy))
        val r = rows
        if (r == null) Muted(stringResource(R.string.psy_loading)) else if (r.isEmpty()) Muted(stringResource(R.string.psy_no_messages))
        r?.forEach { (id, title, other) -> Panel(Modifier.clickable { go(PS.Chat(id, title, other)) }) { Text(title, fontWeight = FontWeight.Bold, color = OkBlack) } }
    }
}

@Composable
private fun ChatScreen(convId: String, title: String, otherId: String) {
    val uid = myId() ?: return
    val repo = LocalDataRepository.current
    val scope = rememberCoroutineScope()
    val msgs = remember { mutableStateListOf<ChatMessage>() }
    var text by remember { mutableStateOf("") }
    var err by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()

    fun merge(list: List<ChatMessage>) { list.forEach { m -> if (msgs.none { it.id == m.id }) msgs.add(m) }; msgs.sortBy { it.id } }
    LaunchedEffect(convId) {
        merge(PsychRepo.messages(convId) ?: emptyList())
        while (true) { delay(10_000); PsychRepo.messages(convId, msgs.lastOrNull()?.id ?: 0)?.let { merge(it) } } // fallback poll
    }
    DisposableEffect(convId) {
        // Supabase Realtime pushes new rows (RLS-filtered: only the two parties can receive them).
        val off = PsychRepo.subscribeMessages(convId) { m -> merge(listOf(m)) }
        onDispose { off() }
    }
    LaunchedEffect(msgs.size) { scroll.animateScrollTo(scroll.maxValue) }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = OkBlack, modifier = Modifier.weight(1f))
            TextButton(onClick = { scope.launch { repo.reportContent("user", otherId, "other") } }) { Text(stringResource(R.string.psy_report), fontSize = 12.sp, color = OkMuted) }
        }
        Muted(stringResource(R.string.psy_chat_privacy), Modifier.padding(horizontal = 16.dp))
        Column(Modifier.weight(1f).verticalScroll(scroll).padding(12.dp)) {
            msgs.forEach { m ->
                val mine = m.sender_id == uid
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    Column(
                        Modifier.clip(RoundedCornerShape(14.dp)).background(if (mine) OkOrangeLight else OkSurface)
                            .border(1.dp, OkBorder, RoundedCornerShape(14.dp)).padding(10.dp).fillMaxWidth(0.8f)
                    ) { Text(m.body, color = OkBlack, fontSize = 14.sp); Muted(fmtTime(m.created_at)) }
                }
            }
        }
        if (err) Text(stringResource(R.string.psy_error_generic), color = OkOrangeShade, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(text, { text = it.take(4000) }, Modifier.weight(1f), placeholder = { Text(stringResource(R.string.psy_type_message)) }, maxLines = 4)
            Spacer(Modifier.width(6.dp))
            Btn(stringResource(R.string.psy_send), {
                val body = text.trim()
                if (body.isNotEmpty()) scope.launch {
                    if (PsychRepo.sendMessage(convId, uid, body)) { text = ""; err = false; merge(PsychRepo.messages(convId, msgs.lastOrNull()?.id ?: 0) ?: emptyList()) } else err = true
                }
            }, enabled = text.isNotBlank())
        }
    }
}

// ------------------------------------------------------------------------------------------
// "Share with my psychologist": explicit, scoped, time-boxed, revocable; server logs every access
// ------------------------------------------------------------------------------------------
@Composable
private fun ShareWithPsychologistScreen() {
    val uid = myId() ?: return
    val ctx = LocalContext.current
    val repo = LocalDataRepository.current
    val scope = rememberCoroutineScope()
    var psychs by remember { mutableStateOf<List<PsychProfile>>(emptyList()) }
    var grants by remember { mutableStateOf<List<ShareGrant>>(emptyList()) }
    var pick by remember { mutableStateOf<String?>(null) }
    var scopes by remember { mutableStateOf(setOf("mood")) }
    var days by remember { mutableIntStateOf(7) }
    var consent by remember { mutableStateOf(false) }
    var pickedJournal by remember { mutableStateOf(setOf<String>()) }
    var audit by remember { mutableStateOf<Pair<String, List<com.cynorra.areweokay.data.AuditRow>>?>(null) }
    var msg by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    val journal by repo.local.journal.collectAsState()

    LaunchedEffect(tick) {
        grants = PsychRepo.grants(uid)
        val ids = (PsychRepo.appointments(uid).map { it.psychologist_id } + PsychRepo.myConversations(uid).map { it.psychologist_id }).filter { it != uid }.toSet()
        psychs = PsychRepo.profilesByIds(ids).values.filter { it.verified }
    }
    fun journalFor() = journal.filter { it.id in pickedJournal }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        H(stringResource(R.string.psy_share_title))
        Muted(stringResource(R.string.psy_share_intro))
        Spacer(Modifier.height(8.dp))
        if (psychs.isEmpty()) Muted(stringResource(R.string.psy_share_none))
        FlowRow { psychs.forEach { p -> Pill(p.display_name, pick == p.user_id) { pick = p.user_id } } }
        if (pick != null) Panel {
            Text(stringResource(R.string.psy_share_what), fontWeight = FontWeight.Bold)
            FlowRow {
                Pill(stringResource(R.string.psy_scope_mood), "mood" in scopes) { scopes = scopes + "mood" }
                Pill(stringResource(R.string.psy_scope_emotions), "emotions" in scopes) { scopes = if ("emotions" in scopes) scopes - "emotions" else scopes + "emotions" }
                Pill(stringResource(R.string.psy_scope_journal), "journal" in scopes) { scopes = if ("journal" in scopes) scopes - "journal" else scopes + "journal" }
            }
            if ("journal" in scopes) {
                Muted(stringResource(R.string.psy_share_journal_pick))
                journal.take(20).forEach { j ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(j.id in pickedJournal, { pickedJournal = if (it) pickedJournal + j.id else pickedJournal - j.id })
                        Text(j.body.take(70), fontSize = 12.sp, color = OkBlack, maxLines = 2)
                    }
                }
            }
            Text(stringResource(R.string.psy_share_for), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
            FlowRow { listOf(7, 30, 90).forEach { d -> Pill(stringResource(R.string.psy_days, d), days == d) { days = d } } }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(consent, { consent = it })
                Text(stringResource(R.string.psy_share_consent), fontSize = 12.sp, color = OkBlack, modifier = Modifier.weight(1f))
            }
            Btn(stringResource(R.string.psy_share_start), {
                scope.launch {
                    val g = PsychRepo.createGrant(uid, pick!!, scopes.toList(), days)
                    msg = if (g != null && PsychRepo.pushSnapshot(uid, g, journalFor())) ctx.getString(R.string.psy_share_done) else ctx.getString(R.string.psy_error_generic)
                    consent = false; tick++
                }
            }, Modifier.fillMaxWidth(), enabled = consent && pick != null)
        }
        msg?.let { Text(it, color = OkTeal, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)) }

        Spacer(Modifier.height(12.dp))
        H(stringResource(R.string.psy_share_active))
        val names = psychs.associate { it.user_id to it.display_name }
        grants.filter { it.active }.forEach { g ->
            Panel {
                Text(names[g.psychologist_id] ?: "?", fontWeight = FontWeight.Bold)
                Muted(stringResource(R.string.psy_share_until, fmtTime(g.expires_at)) + " · " + g.scopes.joinToString())
                FlowRow(Modifier.padding(top = 6.dp)) {
                    Pill(stringResource(R.string.psy_share_refresh), false) { scope.launch { PsychRepo.pushSnapshot(uid, g, journalFor()); msg = ctx.getString(R.string.psy_share_done) } }
                    Pill(stringResource(R.string.psy_share_log), false) { scope.launch { audit = g.id to PsychRepo.audit(g.id) } }
                    Pill(stringResource(R.string.psy_share_revoke), true) { scope.launch { PsychRepo.revokeGrant(g.id); tick++ } }
                }
            }
        }
        if (grants.none { it.active }) Muted(stringResource(R.string.psy_share_noactive))
    }
    audit?.let { (_, rows) ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { audit = null },
            title = { Text(stringResource(R.string.psy_share_log)) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { rows.forEach { Text(fmtTime(it.created_at) + "  ·  " + it.action, fontSize = 12.sp) } } },
            confirmButton = { TextButton(onClick = { audit = null }) { Text(stringResource(R.string.close)) } }
        )
    }
}

/** Builds a safe https URL from a handle or a pasted link; only http(s) is ever opened. */
private fun socialUrl(base: String, raw: String): String {
    val v = raw.trim().removePrefix("@")
    if (v.startsWith("https://") || v.startsWith("http://")) return v
    return base + v.removePrefix("https://").removePrefix("http://")
}
