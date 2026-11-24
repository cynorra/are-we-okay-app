package com.cynorra.areweokay.ui.wellbeing

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cynorra.areweokay.R
import com.cynorra.areweokay.data.LocalDataRepository
import com.cynorra.areweokay.notify.ReminderScheduler
import com.cynorra.areweokay.theme.OkBlack
import com.cynorra.areweokay.theme.OkBorder
import com.cynorra.areweokay.theme.OkMuted
import com.cynorra.areweokay.theme.OkOrange
import com.cynorra.areweokay.theme.OkSurface
import kotlinx.coroutines.launch
import java.util.Locale

private val Danger = Color(0xFFC62828)

// ------------------------------------------------------------------ HUB (check-in tab)

@Composable
fun WellbeingHub(onOpen: (Overlay) -> Unit) {
    val repo = LocalDataRepository.current
    var streak by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { streak = repo.getUserStats().streak }
    Column(Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = OkSurface),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, OkBorder)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("🔥 " + stringResource(R.string.streak_days, streak), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = OkBlack)
                Spacer(Modifier.height(12.dp))
                BadgesRow(streak = streak, checkinCount = repo.checkinCount())
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { onOpen(Overlay.Journal) }, modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, OkBorder)
            ) { Text("📓 " + stringResource(R.string.journal_title), color = OkBlack, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            OutlinedButton(
                onClick = { onOpen(Overlay.Breathing) }, modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, OkBorder)
            ) { Text("🌬️ " + stringResource(R.string.breath_title), color = OkBlack, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        }
    }
}

// ------------------------------------------------------------------ SETTINGS: reminder, invite, privacy, delete

@Composable
fun PrivacyAndRemindersCard(onAccountDeleted: () -> Unit) {
    val repo = LocalDataRepository.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var reminderOn by remember { mutableStateOf(repo.local.reminderEnabled) }
    var hour by remember { mutableIntStateOf(repo.local.reminderHour) }
    var confirmDelete by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf(false) }

    fun applyReminder(on: Boolean) {
        reminderOn = on
        repo.local.reminderEnabled = on
        if (on) ReminderScheduler.schedule(ctx, hour) else ReminderScheduler.cancel(ctx)
    }
    // If the permission is denied the worker stays silent (it re-checks the permission before notifying).
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
        applyReminder(true)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = OkSurface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, OkBorder)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.reminder_title), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = OkBlack)
                    Text(stringResource(R.string.reminder_desc), fontSize = 12.sp, color = OkMuted)
                }
                Switch(
                    checked = reminderOn,
                    onCheckedChange = { on ->
                        if (on && Build.VERSION.SDK_INT >= 33) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else applyReminder(on)
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = OkOrange)
                )
            }
            if (reminderOn) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { hour = (hour + 23) % 24; repo.local.reminderHour = hour; ReminderScheduler.schedule(ctx, hour) }) { Text("-", fontSize = 20.sp) }
                    Text(String.format(Locale.getDefault(), "%02d:00", hour), fontWeight = FontWeight.Bold, color = OkBlack)
                    TextButton(onClick = { hour = (hour + 1) % 24; repo.local.reminderHour = hour; ReminderScheduler.schedule(ctx, hour) }) { Text("+", fontSize = 20.sp) }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { ShareCard.invite(ctx, repo.getCurrentUserSync()?.id) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OkOrange),
                shape = RoundedCornerShape(12.dp)
            ) { Text("💌 " + stringResource(R.string.invite_friends), fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ctx.getString(R.string.privacy_policy_url)))) }) {
                Text(stringResource(R.string.consent_privacy_link), color = OkOrange, fontWeight = FontWeight.Bold)
            }
            Text(stringResource(R.string.disclaimer_short), fontSize = 12.sp, color = OkMuted)
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Danger)
            ) { Text(stringResource(R.string.delete_account), color = Danger, fontWeight = FontWeight.Bold) }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { if (!deleting) confirmDelete = false },
            title = { Text(stringResource(R.string.delete_account)) },
            text = {
                Column {
                    Text(stringResource(R.string.delete_account_body))
                    if (deleteError) Text(stringResource(R.string.delete_failed), color = Danger, modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = {
                TextButton(enabled = !deleting, onClick = {
                    deleting = true; deleteError = false
                    scope.launch {
                        val r = repo.deleteAccount()
                        deleting = false
                        if (r.isSuccess) { confirmDelete = false; onAccountDeleted() } else deleteError = true
                    }
                }) { Text(stringResource(R.string.delete_account_confirm), color = Danger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(enabled = !deleting, onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

// ------------------------------------------------------------------ REPORT / BLOCK

@Composable
fun ReportDialog(onDismiss: () -> Unit, onReport: (String) -> Unit, onBlock: () -> Unit) {
    var doneRes by remember { mutableStateOf<Int?>(null) }
    val reasons = listOf(
        "spam" to R.string.report_spam, "harassment" to R.string.report_harassment,
        "self_harm" to R.string.report_self_harm, "other" to R.string.report_other
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.report_title)) },
        text = {
            Column {
                val d = doneRes
                if (d != null) {
                    Text(stringResource(d))
                } else {
                    reasons.forEach { (key, res) ->
                        TextButton(onClick = { onReport(key); doneRes = R.string.report_thanks }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(res), color = OkBlack, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    TextButton(onClick = { onBlock(); doneRes = R.string.block_done }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.block_user), color = Danger, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(if (doneRes != null) R.string.close else R.string.cancel)) } }
    )
}
