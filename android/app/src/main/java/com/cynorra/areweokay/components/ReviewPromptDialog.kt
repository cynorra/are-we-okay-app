package com.cynorra.areweokay.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.delay
import java.util.Locale

private const val PREFS = "app_review_prompt_v1"
private const val KEY_LAUNCHES = "launches"
private const val KEY_HAS_RATED = "has_rated"
private const val KEY_NEVER_ASK = "never_ask"
private const val KEY_PROMPT_COUNT = "prompt_count"
private const val KEY_LAST_PROMPT_AT = "last_prompt_at"
private const val MIN_LAUNCHES = 2
private const val MAX_PROMPTS = 2
private const val COOLDOWN_MS = 21L * 24L * 60L * 60L * 1000L

@Composable
fun ReviewPromptDialog(activity: Activity) {
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val launches = prefs.getInt(KEY_LAUNCHES, 0) + 1
        prefs.edit().putInt(KEY_LAUNCHES, launches).apply()

        val hasRated = prefs.getBoolean(KEY_HAS_RATED, false)
        val neverAsk = prefs.getBoolean(KEY_NEVER_ASK, false)
        val promptCount = prefs.getInt(KEY_PROMPT_COUNT, 0)
        val lastPrompt = prefs.getLong(KEY_LAST_PROMPT_AT, 0L)
        val now = System.currentTimeMillis()

        if (!hasRated && !neverAsk && launches >= MIN_LAUNCHES && promptCount < MAX_PROMPTS &&
            (lastPrompt == 0L || (now - lastPrompt) >= COOLDOWN_MS)
        ) {
            delay(6000L)
            if (!activity.isFinishing && !activity.isDestroyed) {
                showDialog = true
            }
        }
    }

    if (!showDialog) return

    val isTr = Locale.getDefault().language.equals("tr", ignoreCase = true)
    val prefs = remember { activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    AlertDialog(
        onDismissRequest = {
            showDialog = false
            val count = prefs.getInt(KEY_PROMPT_COUNT, 0) + 1
            prefs.edit()
                .putInt(KEY_PROMPT_COUNT, count)
                .putLong(KEY_LAST_PROMPT_AT, System.currentTimeMillis())
                .apply()
        },
        title = {
            Text(if (isTr) "Uygulamamızı Beğeniyor musunuz?" else "Enjoying Are We Okay?")
        },
        text = {
            Text(
                if (isTr) {
                    "Are We Okay'i geliştirmemize destek olmak için Google Play'de değerlendirmenizi paylaşabilirsiniz."
                } else {
                    "Your review on Google Play helps us improve Are We Okay for everyone."
                }
            )
        },
        confirmButton = {
            Button(onClick = {
                showDialog = false
                prefs.edit()
                    .putBoolean(KEY_HAS_RATED, true)
                    .putLong(KEY_LAST_PROMPT_AT, System.currentTimeMillis())
                    .apply()
                launchInAppReviewOrStore(activity)
            }) {
                Text(if (isTr) "Değerlendir" else "Rate App")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                showDialog = false
                prefs.edit()
                    .putBoolean(KEY_NEVER_ASK, true)
                    .putLong(KEY_LAST_PROMPT_AT, System.currentTimeMillis())
                    .apply()
            }) {
                Text(if (isTr) "Bir Daha Gösterme" else "Don't Ask Again")
            }
        }
    )
}

private fun launchInAppReviewOrStore(activity: Activity) {
    try {
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful && !activity.isFinishing && !activity.isDestroyed) {
                manager.launchReviewFlow(activity, task.result)
            } else {
                openPlayStore(activity)
            }
        }
    } catch (_: Exception) {
        openPlayStore(activity)
    }
}

private fun openPlayStore(activity: Activity) {
    val pkg = activity.packageName
    try {
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")))
    } catch (_: Exception) {
        try {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg"))
            )
        } catch (_: Exception) {
        }
    }
}
