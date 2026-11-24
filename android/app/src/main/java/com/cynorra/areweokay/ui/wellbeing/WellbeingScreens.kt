package com.cynorra.areweokay.ui.wellbeing

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.TelephonyManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import com.cynorra.areweokay.notify.ReminderScheduler
import com.cynorra.areweokay.data.MoodScale
import kotlinx.coroutines.launch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cynorra.areweokay.R
import com.cynorra.areweokay.components.NoAds
import com.cynorra.areweokay.data.LocalDataRepository
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
import java.text.DateFormat
import java.util.Date
import java.util.Locale

enum class Overlay { Journal, Breathing, Crisis, Psych }

/** Full-screen sensitive page: no banner ad, back arrow, scrollable body. */
@Composable
private fun SensitiveScaffold(title: String, onClose: () -> Unit, content: @Composable () -> Unit) {
    NoAds()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OkBeige)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OkSurface)
                .border(1.dp, OkBorder)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onClose) { Text("←", fontSize = 22.sp, color = OkBlack) }
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = OkBlack)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) { content() }
    }
}

// ------------------------------------------------------------------ CRISIS / HELP

private data class Helpline(val name: String, val phone: String)

// Well-known national lines. Always show "verify locally" copy; fallback links to findahelpline.com.
private val helplines: Map<String, List<Helpline>> = mapOf(
    "US" to listOf(Helpline("988 Suicide & Crisis Lifeline", "988")),
    "CA" to listOf(Helpline("9-8-8 Suicide Crisis Helpline", "988")),
    "GB" to listOf(Helpline("Samaritans", "116123")),
    "IE" to listOf(Helpline("Samaritans", "116123")),
    "AU" to listOf(Helpline("Lifeline", "131114")),
    "NZ" to listOf(Helpline("1737 Need to talk?", "1737")),
    "IN" to listOf(Helpline("Tele-MANAS", "14416")),
    "DE" to listOf(Helpline("Telefonseelsorge", "08001110111")),
    "FR" to listOf(Helpline("3114", "3114")),
    "ES" to listOf(Helpline("Línea 024", "024")),
    "BR" to listOf(Helpline("CVV", "188")),
    "MX" to listOf(Helpline("Línea de la Vida", "8009112000")),
    "ZA" to listOf(Helpline("SADAG", "0800567567")),
    "TR" to listOf(Helpline("112 Acil / Emergency", "112"), Helpline("ALO 183 Sosyal Destek", "183"))
)

private fun detectCountry(ctx: Context): String {
    val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    val c = tm?.networkCountryIso?.takeIf { it.isNotBlank() }
        ?: tm?.simCountryIso?.takeIf { it.isNotBlank() }
        ?: Locale.getDefault().country
    return c.uppercase(Locale.ROOT)
}

@Composable
fun CrisisScreen(onClose: () -> Unit, onBreathe: () -> Unit) {
    val ctx = LocalContext.current
    val country = remember { detectCountry(ctx) }
    val lines = helplines[country].orEmpty()
    SensitiveScaffold(stringResource(R.string.crisis_title), onClose) {
        Text(stringResource(R.string.crisis_intro), fontSize = 16.sp, color = OkBlack, lineHeight = 24.sp)
        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OkOrangeLight),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, OkOrange)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.crisis_emergency), fontWeight = FontWeight.Bold, color = OkOrangeShade)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.crisis_emergency_body), fontSize = 14.sp, color = OkBlack)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (lines.isNotEmpty()) stringResource(R.string.crisis_lines_for, country) else stringResource(R.string.crisis_no_lines),
            fontWeight = FontWeight.Bold, color = OkBlack
        )
        Spacer(Modifier.height(8.dp))
        lines.forEach { l ->
            Button(
                onClick = { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${l.phone}"))) },
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(bottom = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OkTeal),
                shape = RoundedCornerShape(14.dp)
            ) { Text("📞 ${l.name}", fontWeight = FontWeight.Bold) }
        }
        Button(
            onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://findahelpline.com"))) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OkBlack),
            shape = RoundedCornerShape(14.dp)
        ) { Text(stringResource(R.string.crisis_find_more), fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.crisis_verify), fontSize = 12.sp, color = OkMuted)
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.crisis_now), fontWeight = FontWeight.Bold, color = OkBlack)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.crisis_grounding), fontSize = 14.sp, color = OkBlack, lineHeight = 22.sp)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBreathe) {
            Text("🌬️ " + stringResource(R.string.crisis_breathe), fontWeight = FontWeight.Bold, color = OkOrangeShade)
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.disclaimer_short), fontSize = 12.sp, color = OkMuted)
    }
}

// ------------------------------------------------------------------ BREATHING

private enum class PhaseKind { In, Hold, Out }
private data class Phase(val kind: PhaseKind, val seconds: Int)
private data class Technique(val nameRes: Int, val descRes: Int, val phases: List<Phase>)

private val techniques = listOf(
    Technique(
        R.string.breath_box, R.string.breath_box_desc,
        listOf(Phase(PhaseKind.In, 4), Phase(PhaseKind.Hold, 4), Phase(PhaseKind.Out, 4), Phase(PhaseKind.Hold, 4))
    ),
    Technique(
        R.string.breath_478, R.string.breath_478_desc,
        listOf(Phase(PhaseKind.In, 4), Phase(PhaseKind.Hold, 7), Phase(PhaseKind.Out, 8))
    ),
    Technique(
        R.string.breath_calm, R.string.breath_calm_desc,
        listOf(Phase(PhaseKind.In, 4), Phase(PhaseKind.Out, 6))
    )
)

@Composable
fun BreathingScreen(onClose: () -> Unit) {
    val repo = LocalDataRepository.current
    var selected by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var cycles by remember { mutableIntStateOf(0) }
    var phaseKind by remember { mutableStateOf<PhaseKind?>(null) }
    var secondsLeft by remember { mutableIntStateOf(0) }
    val scale = remember { Animatable(0.55f) }
    val tech = techniques[selected]

    fun finish() {
        if (cycles >= 1) repo.local.breathingSessions = repo.local.breathingSessions + 1
        running = false
        cycles = 0
        phaseKind = null
    }

    LaunchedEffect(running, selected) {
        if (!running) { scale.snapTo(0.55f); return@LaunchedEffect }
        while (true) {
            for (ph in tech.phases) {
                phaseKind = ph.kind
                secondsLeft = ph.seconds
                if (ph.kind == PhaseKind.Hold) {
                    repeat(ph.seconds) { delay(1000); secondsLeft-- }
                } else {
                    val target = if (ph.kind == PhaseKind.In) 1f else 0.55f
                    scale.animateTo(target, tween(ph.seconds * 1000, easing = LinearEasing))
                }
            }
            cycles++
        }
    }
    // Separate ticker for the visible countdown during in/out phases.
    LaunchedEffect(phaseKind, running) {
        if (!running || phaseKind == null || phaseKind == PhaseKind.Hold) return@LaunchedEffect
        while (secondsLeft > 1) { delay(1000); secondsLeft-- }
    }

    SensitiveScaffold(stringResource(R.string.breath_title), { finish(); onClose() }) {
        Text(stringResource(R.string.breath_intro), fontSize = 15.sp, color = OkMuted)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            techniques.forEachIndexed { i, t ->
                val sel = i == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (sel) OkOrangeLight else OkSurface)
                        .border(1.dp, if (sel) OkOrange else OkBorder, RoundedCornerShape(14.dp))
                        .clickable(enabled = !running) { selected = i }
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(t.nameRes), fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = OkBlack)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(tech.descRes), fontSize = 13.sp, color = OkMuted)
        Spacer(Modifier.height(32.dp))
        Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .scale(scale.value)
                    .clip(CircleShape)
                    .background(OkTealLight)
                    .border(3.dp, OkTeal, CircleShape)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    when (phaseKind) {
                        PhaseKind.In -> stringResource(R.string.breath_in)
                        PhaseKind.Hold -> stringResource(R.string.breath_hold)
                        PhaseKind.Out -> stringResource(R.string.breath_out)
                        null -> stringResource(R.string.breath_ready)
                    },
                    fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = OkBlack
                )
                if (running && phaseKind != null) Text("$secondsLeft", fontSize = 18.sp, color = OkMuted)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.breath_cycles, cycles),
            fontSize = 13.sp, color = OkMuted, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { if (running) finish() else running = true },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (running) OkBlack else OkOrange),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(if (running) stringResource(R.string.breath_stop) else stringResource(R.string.breath_start), fontWeight = FontWeight.Bold)
        }
    }
}

// ------------------------------------------------------------------ JOURNAL

private data class Template(val key: String, val nameRes: Int, val hintRes: Int)

private val templates = listOf(
    Template("free", R.string.journal_t_free, R.string.journal_h_free),
    Template("gratitude", R.string.journal_t_gratitude, R.string.journal_h_gratitude),
    Template("thought", R.string.journal_t_thought, R.string.journal_h_thought),
    Template("tomorrow", R.string.journal_t_tomorrow, R.string.journal_h_tomorrow)
)

@Composable
fun JournalScreen(onClose: () -> Unit) {
    val repo = LocalDataRepository.current
    val entries by repo.local.journal.collectAsState()
    var composing by remember { mutableStateOf(false) }
    var template by remember { mutableStateOf(templates[0]) }
    var body by remember { mutableStateOf("") }
    val df = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    SensitiveScaffold(stringResource(R.string.journal_title), { if (composing) composing = false else onClose() }) {
        Text("🔒 " + stringResource(R.string.journal_private), fontSize = 13.sp, color = OkMuted)
        Spacer(Modifier.height(12.dp))
        if (composing) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                templates.forEach { t ->
                    val sel = t.key == template.key
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) OkOrangeLight else OkSurface)
                            .border(1.dp, if (sel) OkOrange else OkBorder, RoundedCornerShape(12.dp))
                            .clickable { template = t }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(stringResource(t.nameRes), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = OkBlack) }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                placeholder = { Text(stringResource(template.hintRes), color = OkMuted) },
                modifier = Modifier.fillMaxWidth().height(220.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OkOrange, unfocusedBorderColor = OkBorder)
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    if (body.isNotBlank()) {
                        repo.local.addJournal(template.key, body.trim(), repo.lastCheckin()?.level)
                        body = ""
                        composing = false
                    }
                },
                enabled = body.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OkBlack),
                shape = RoundedCornerShape(16.dp)
            ) { Text(stringResource(R.string.journal_save), fontWeight = FontWeight.Bold) }
        } else {
            Button(
                onClick = { composing = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OkOrange),
                shape = RoundedCornerShape(16.dp)
            ) { Text("✍️ " + stringResource(R.string.journal_new), fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(16.dp))
            if (entries.isEmpty()) {
                Text(stringResource(R.string.journal_empty), color = OkMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            entries.forEach { e ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    colors = CardDefaults.cardColors(containerColor = OkSurface),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, OkBorder)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                (e.mood_level?.let { com.cynorra.areweokay.data.MoodScale.emojis[(it - 1).coerceIn(0, 4)] + " " } ?: "") +
                                    df.format(Date(e.created_at)),
                                fontSize = 12.sp, color = OkMuted, modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { repo.local.deleteJournal(e.id) }) {
                                Text(stringResource(R.string.delete), color = Color(0xFFC62828), fontSize = 12.sp)
                            }
                        }
                        Text(e.body, fontSize = 15.sp, color = OkBlack, lineHeight = 22.sp)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ BADGES

@Composable
fun BadgesRow(streak: Int, checkinCount: Int) {
    val repo = LocalDataRepository.current
    val journal by repo.local.journal.collectAsState() // recompose when journal changes
    val badges = remember(streak, checkinCount, journal.size, repo.local.breathingSessions) { repo.local.badges(streak, checkinCount) }
    val names = mapOf(
        "first_checkin" to R.string.badge_first, "streak_3" to R.string.badge_s3, "streak_7" to R.string.badge_s7,
        "streak_14" to R.string.badge_s14, "streak_30" to R.string.badge_s30, "journal_1" to R.string.badge_j1,
        "journal_10" to R.string.badge_j10, "breath_1" to R.string.badge_b1
    )
    Column(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.badges_title), fontWeight = FontWeight.Bold, color = OkBlack, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        badges.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { b ->
                    Column(Modifier.width(76.dp).padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (b.earned) b.emoji else "🔒", fontSize = 28.sp, color = if (b.earned) Color.Unspecified else OkMuted)
                        Text(
                            stringResource(names[b.key] ?: R.string.badges_title), fontSize = 10.sp, textAlign = TextAlign.Center,
                            color = if (b.earned) OkBlack else OkMuted, maxLines = 2
                        )
                    }
                }
            }
        }
    }
}
