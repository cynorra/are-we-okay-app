package com.cynorra.areweokay.ui.psych

import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cynorra.areweokay.R
import com.cynorra.areweokay.data.PsychRepo
import com.cynorra.areweokay.theme.OkBlack
import com.cynorra.areweokay.theme.OkBorder
import com.cynorra.areweokay.theme.OkMuted
import com.cynorra.areweokay.theme.OkOrange
import com.cynorra.areweokay.theme.OkOrangeLight
import com.cynorra.areweokay.theme.OkSurface
import com.cynorra.areweokay.theme.OkTeal
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Screens inside the psychologist hub (simple in-hub back stack). */
sealed interface PS {
    data object Directory : PS
    data class Profile(val id: String) : PS
    data class Book(val id: String) : PS
    data object Appts : PS
    data object Messages : PS
    data class Chat(val convId: String, val title: String, val otherId: String) : PS
    data object Onboard : PS
    data object Dashboard : PS
    data object Clients : PS
    data class Client(val clientId: String, val name: String, val convId: String?, val grantId: String?) : PS
    data object Avail : PS
    data object Compose : PS
    data object Share : PS
}

val SPECIALTIES: List<Pair<String, Int>> = listOf(
    "anxiety" to R.string.psy_spec_anxiety, "depression" to R.string.psy_spec_depression,
    "trauma" to R.string.psy_spec_trauma, "relationships" to R.string.psy_spec_relationships,
    "stress" to R.string.psy_spec_stress, "grief" to R.string.psy_spec_grief,
    "addiction" to R.string.psy_spec_addiction, "adhd" to R.string.psy_spec_adhd,
    "eating" to R.string.psy_spec_eating, "sleep" to R.string.psy_spec_sleep,
    "youth" to R.string.psy_spec_youth, "couples" to R.string.psy_spec_couples,
    "career" to R.string.psy_spec_career
)

@StringRes
fun specRes(key: String): Int? = SPECIALTIES.firstOrNull { it.first == key }?.second

val DIR_LANGS = listOf("en", "tr", "de", "fr", "es", "ar", "ru", "pt", "it", "nl", "fa", "hi", "zh", "ja", "ko")

fun langName(code: String): String = Locale(code).getDisplayLanguage(Locale.getDefault()).replaceFirstChar { it.uppercase() }
fun countryName(code: String): String = Locale("", code).getDisplayCountry(Locale.getDefault()).ifBlank { code }

private val timeFmt = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
fun fmtTime(iso: String): String =
    runCatching { timeFmt.withZone(ZoneId.systemDefault()).format(PsychRepo.parse(iso)) }.getOrDefault(iso)

fun minToHm(m: Int) = "%02d:%02d".format(m / 60, m % 60)

fun qrBitmap(text: String, size: Int = 512): Bitmap {
    val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (x in 0 until size) for (y in 0 until size) bmp.setPixel(x, y, if (m.get(x, y)) 0xFF402E2A.toInt() else 0xFFFFFFFF.toInt())
    return bmp
}

@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.padding(end = 6.dp, bottom = 6.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) OkOrange else OkSurface)
            .border(1.dp, if (selected) OkOrange else OkBorder, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) { Text(text, fontSize = 12.sp, color = if (selected) Color.White else OkBlack) }
}

@Composable
fun Tag(text: String, color: Color = OkTeal) {
    Text(text, fontSize = 11.sp, color = color, modifier = Modifier.padding(end = 6.dp)
        .background(OkOrangeLight, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp))
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = OkSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, OkBorder)
    ) { Column(Modifier.padding(14.dp), content = content) }
}

@Composable
fun Btn(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, primary: Boolean = true) {
    if (primary) Button(
        onClick = onClick, enabled = enabled, modifier = modifier.height(44.dp), shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = OkOrange)
    ) { Text(text, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White) }
    else OutlinedButton(
        onClick = onClick, enabled = enabled, modifier = modifier.height(44.dp), shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, OkBorder)
    ) { Text(text, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = OkBlack) }
}

@Composable
fun H(text: String) = Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = OkBlack, modifier = Modifier.padding(vertical = 6.dp))

@Composable
fun Muted(text: String, modifier: Modifier = Modifier) = Text(text, fontSize = 12.sp, color = OkMuted, lineHeight = 17.sp, modifier = modifier)

@Composable
fun VerifiedBadge() = Text("✔ " + stringResource(R.string.psy_verified), fontSize = 11.sp, color = OkTeal, fontWeight = FontWeight.Bold)
