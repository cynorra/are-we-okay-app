package com.cynorra.areweokay.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.cynorra.areweokay.MainActivity
import com.cynorra.areweokay.R
import com.cynorra.areweokay.data.Appointment
import com.cynorra.areweokay.data.PsychRepo
import java.util.concurrent.TimeUnit

/**
 * Local reminder shortly before a confirmed session. The notification text is generic on purpose
 * (no names, no health details on the lock screen). Push for requests/messages while the app is closed
 * needs FCM + a server webhook and is NOT part of this phase.
 */
class AppointmentReminderWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, applicationContext.getString(R.string.psy_notif_channel), NotificationManager.IMPORTANCE_HIGH))
        }
        val open = PendingIntent.getActivity(
            applicationContext, 0, Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(applicationContext, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(applicationContext.getString(R.string.app_name))
            .setContentText(applicationContext.getString(R.string.psy_notif_text))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(2000 + (inputData.getString("id")?.hashCode()?.and(0xFFF) ?: 0), n)
        return Result.success()
    }

    companion object { const val CHANNEL = "appointments" }
}

object AppointmentReminders {
    private const val LEAD_MIN = 30L

    /** Schedules (or cancels, when the session is no longer confirmed) one reminder per appointment. */
    fun sync(ctx: Context, appts: List<Appointment>) {
        val wm = WorkManager.getInstance(ctx)
        val now = System.currentTimeMillis()
        appts.forEach { a ->
            val name = "appt_${a.id}"
            val at = runCatching { PsychRepo.parse(a.start_at).toEpochMilli() }.getOrNull()
            if (a.status != "confirmed" || at == null) { wm.cancelUniqueWork(name); return@forEach }
            val delay = at - LEAD_MIN * 60_000 - now
            if (delay <= 0) return@forEach
            val req = OneTimeWorkRequestBuilder<AppointmentReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(Data.Builder().putString("id", a.id).build())
                .build()
            wm.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, req)
        }
    }
}
