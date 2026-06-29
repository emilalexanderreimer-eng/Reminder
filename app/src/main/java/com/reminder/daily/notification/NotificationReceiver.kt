package com.reminder.daily.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.reminder.daily.MainActivity
import com.reminder.daily.R
import com.reminder.daily.data.FirestoreRepository
import com.reminder.daily.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "reminder_channel"
        const val EXTRA_TIME_OF_DAY = "time_of_day"
        const val EXTRA_HOUR = "hour"
        const val EXTRA_MINUTE = "minute"
        const val MORNING = 0
        const val MIDDAY = 1
        const val EVENING = 2
    }

    override fun onReceive(context: Context, intent: Intent) {
        val timeOfDay = intent.getIntExtra(EXTRA_TIME_OF_DAY, MORNING)
        val hour = intent.getIntExtra(EXTRA_HOUR, 8)
        val minute = intent.getIntExtra(EXTRA_MINUTE, 0)

        // Reschedule for tomorrow
        NotificationScheduler.schedule(context, hour, minute, timeOfDay)

        val listId = Prefs.getListId(context) ?: return

        val pending = goAsync()
        val scope = CoroutineScope(Dispatchers.IO + Job())
        scope.launch {
            try {
                val count = FirestoreRepository(listId).getPendingCount()
                if (count > 0) showNotification(context, timeOfDay, count)
            } finally {
                pending.finish()
                scope.cancel()
            }
        }
    }

    private fun showNotification(context: Context, timeOfDay: Int, count: Int) {
        val greeting = when (timeOfDay) {
            MORNING -> "Guten Morgen!"
            MIDDAY -> "Mittagserinnerung"
            else -> "Guten Abend!"
        }
        val taskWord = if (count == 1) "Aufgabe" else "Aufgaben"

        ensureNotificationChannel(context)

        val openIntent = PendingIntent.getActivity(
            context, timeOfDay + 100,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(greeting)
            .setContentText("Noch $count offene $taskWord in eurer Liste.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Noch $count offene $taskWord in eurer Liste. Tippe um die Liste zu öffnen."))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(timeOfDay, notification)
    }

    private fun ensureNotificationChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Tägliche Erinnerungen", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }
}
