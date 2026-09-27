package com.szainabbas.ummi.reminders

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
import com.szainabbas.ummi.MainActivity
import com.szainabbas.ummi.R
import com.szainabbas.ummi.domain.ReminderEvent

object ReminderNotifier {
    private const val CHANNEL = "reminders"

    /** True when Android will actually show Ummi's notifications. */
    fun allowed(context: Context): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun show(context: Context, event: ReminderEvent) {
        if (!allowed(context)) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "The morning summary, prayer-linked acts, water, tasks and visits you asked to be reminded about."
            },
        )
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_ummi)
            .setColor(0xFF2D4A3E.toInt())
            .setContentTitle(event.title)
            .setContentText(event.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(event.text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(event.key.hashCode(), notification)
    }
}
