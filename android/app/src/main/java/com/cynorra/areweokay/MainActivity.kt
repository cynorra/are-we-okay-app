package com.cynorra.areweokay

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.cynorra.areweokay.data.LocalStore
import com.cynorra.areweokay.data.PsychDeepLink
import com.cynorra.areweokay.notify.ReminderScheduler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.cynorra.areweokay.data.DefaultDataRepository
import com.cynorra.areweokay.data.LocalDataRepository
import com.cynorra.areweokay.theme.AreWeOkayTheme
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    private lateinit var dataRepository: DefaultDataRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        dataRepository = DefaultDataRepository(applicationContext)
        MobileAds.initialize(this) {
            com.cynorra.areweokay.components.AdGate.sdkReady = true
        }
        handleInvite(intent)
        setUpDailyReminder()

        setContent {
            AreWeOkayTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    CompositionLocalProvider(LocalDataRepository provides dataRepository) {
                        MainNavigation()
                    }
                }
            }
        }
    }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // The daily reminder is on by default: make sure it is scheduled and, on Android 13+, ask once for permission.
    private fun setUpDailyReminder() {
        val local = LocalStore(applicationContext)
        if (!local.reminderEnabled) return
        ReminderScheduler.ensure(applicationContext, local.reminderHour)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleInvite(intent)
    }

    // Invite deep link: https://<invite_host>/invite?ref=<inviter uuid> or areweokay://invite?ref=<uuid>
    private fun handleInvite(intent: Intent?) {
        // Psychologist profile link: https://<host>/p/<uuid> or areweokay://psychologist?id=<uuid>
        val uri = intent?.data
        val pid = uri?.getQueryParameter("id") ?: uri?.takeIf { it.path?.startsWith("/p/") == true }?.lastPathSegment
        if (pid != null && Regex("^[0-9a-fA-F-]{36}$").matches(pid) && (uri?.host == "psychologist" || uri?.path?.startsWith("/p/") == true)) {
            PsychDeepLink.pendingId = pid.lowercase(); return
        }
        val ref = intent?.data?.getQueryParameter("ref") ?: return
        if (Regex("^[0-9a-fA-F-]{36}$").matches(ref)) dataRepository.setPendingInviter(ref.lowercase())
    }
}
