package com.islam9lam.namazwidget

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.time.Duration

object PrayerNotification {

    const val NOTIFICATION_ID = 9101
    private const val CHANNEL_ID = "prayer_next"

    fun build(context: Context): Notification? {

        val state =
            PrayerStateCalculator.calculate(context)
                ?: return null

        createChannel(context)

        val now =
            CityClock.localDateTime(context)

        val duration =
            Duration.between(
                now,
                state.nextDateTime
            ).coerceAtLeast(Duration.ZERO)

        val totalMinutes =
            duration.toMinutes()

        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        val remaining =
            when {
                hours > 0 ->
                    "$hours ч $minutes мин"

                totalMinutes > 0 ->
                    "$totalMinutes мин"

                else ->
                    "сейчас"
            }

        val openApp =
            Intent(
                context,
                MainActivity::class.java
            )

        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                openApp,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        return NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(
                android.R.drawable.ic_lock_idle_alarm
            )
            .setContentTitle(
                "${state.nextPrayer.name} · ${state.nextPrayer.time}"
            )
            .setContentText(
                "Осталось $remaining · ${state.city}"
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Следующий намаз: ${state.nextPrayer.name} · " +
                            "${state.nextPrayer.time}\n" +
                            "Осталось: $remaining\n" +
                            state.city
                    )
            )
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(
                NotificationCompat.CATEGORY_EVENT
            )
            .setPriority(
                NotificationCompat.PRIORITY_LOW
            )
            .build()
    }

    fun show(context: Context) {

        if (
            Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification =
            build(context)
                ?: return

        NotificationManagerCompat
            .from(context)
            .notify(
                NOTIFICATION_ID,
                notification
            )
    }

    fun cancel(context: Context) {

        NotificationManagerCompat
            .from(context)
            .cancel(NOTIFICATION_ID)
    }

    private fun createChannel(
        context: Context
    ) {

        if (Build.VERSION.SDK_INT < 26) {
            return
        }

        val manager =
            context.getSystemService(
                NotificationManager::class.java
            )

        if (
            manager.getNotificationChannel(
                CHANNEL_ID
            ) != null
        ) {
            return
        }

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Следующий намаз",
                NotificationManager.IMPORTANCE_LOW
            ).apply {

                description =
                    "Следующий намаз и оставшееся до него время"

                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)

                lockscreenVisibility =
                    Notification.VISIBILITY_PUBLIC
            }

        manager.createNotificationChannel(
            channel
        )
    }
}
