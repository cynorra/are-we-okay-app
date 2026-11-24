package com.cynorra.areweokay.ui.consent

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.cynorra.areweokay.components.NoAds
import com.cynorra.areweokay.data.LocalDataRepository
import com.cynorra.areweokay.theme.OkBeige
import com.cynorra.areweokay.theme.OkBlack
import com.cynorra.areweokay.theme.OkMuted
import com.cynorra.areweokay.theme.OkOrange
import kotlinx.coroutines.launch

/**
 * Explicit consent for processing health-related data (GDPR Art. 9 / KVKK special category) plus an age
 * gate and a "not medical advice" notice. Nothing is pre-checked; the app is unusable until accepted.
 */
@Composable
fun ConsentScreen(onAccepted: () -> Unit, onDeclined: () -> Unit) {
    NoAds()
    val repo = LocalDataRepository.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var health by remember { mutableStateOf(false) }
    var age by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OkBeige)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(stringResource(R.string.consent_title), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = OkBlack)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.consent_body), fontSize = 14.sp, color = OkBlack, lineHeight = 21.sp)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.consent_rights), fontSize = 13.sp, color = OkMuted, lineHeight = 19.sp)
        TextButton(onClick = {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ctx.getString(R.string.privacy_policy_url))))
        }) { Text(stringResource(R.string.consent_privacy_link), color = OkOrange, fontWeight = FontWeight.Bold) }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().clickable { health = !health }, verticalAlignment = Alignment.Top) {
            Checkbox(health, { health = it }, colors = CheckboxDefaults.colors(checkedColor = OkOrange))
            Text(stringResource(R.string.consent_check_health), fontSize = 14.sp, color = OkBlack, modifier = Modifier.padding(top = 12.dp))
        }
        Row(Modifier.fillMaxWidth().clickable { age = !age }, verticalAlignment = Alignment.Top) {
            Checkbox(age, { age = it }, colors = CheckboxDefaults.colors(checkedColor = OkOrange))
            Text(stringResource(R.string.consent_check_age), fontSize = 14.sp, color = OkBlack, modifier = Modifier.padding(top = 12.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.disclaimer_short), fontSize = 12.sp, color = OkMuted)
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { busy = true; scope.launch { repo.recordConsent(); onAccepted() } },
            enabled = health && age && !busy,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OkBlack),
            shape = RoundedCornerShape(16.dp)
        ) { Text(stringResource(R.string.consent_accept), fontWeight = FontWeight.Bold) }
        TextButton(onClick = onDeclined, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.consent_decline), color = OkMuted)
        }
    }
}
