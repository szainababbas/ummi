package com.szainabbas.ummi.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.szainabbas.ummi.MainActivity
import com.szainabbas.ummi.R
import com.szainabbas.ummi.domain.ReminderEvent
import com.szainabbas.ummi.domain.ReminderNotice

/** Posts [ReminderNotice]s. Two channels, so she can silence the daily ones and keep visits. */
object Notifier {
    private const val DAILY = "daily"
    private const val VISITS = "visits"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(DAILY, "Daily reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "The morning summary, acts after each prayer, and water"
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(VISITS, "Visits", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Reminders for appointments and scans"
            },
        )
    }

    fun post(context: Context, notices: List<ReminderNotice>) {
        if (notices.isEmpty()) return
        ensureChannels(context)
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        for (notice in notices) {
            val style = if (notice.lines.size > 1) {
                NotificationCompat.InboxStyle().also { s -> notice.lines.forEach(s::addLine) }
            } else {
                NotificationCompat.BigTextStyle().bigText(notice.lines.first())
            }
            val notification = NotificationCompat.Builder(context, if (notice.channel == ReminderEvent.Channel.VISITS) VISITS else DAILY)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(notice.title)
                .setContentText(notice.lines.joinToString(" · "))
                .setStyle(style)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            // Checked above; a SecurityException means permission went between the check and here.
            try {
                manager.notify(notice.at.hashCode() * 31 + notice.channel.ordinal, notification)
            } catch (e: SecurityException) {
            }
        }
    }
}
