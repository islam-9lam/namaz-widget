package com.islam9lam.namazwidget

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ServiceCompat

class PrayerAodService : Service() {

    private val handler =
        Handler(Looper.getMainLooper())

    private val updater =
        object : Runnable {

            override fun run() {

                val notification =
                    PrayerNotification.build(
                        this@PrayerAodService
                    )

                if (notification != null) {

                    if (Build.VERSION.SDK_INT >= 34) {

                        ServiceCompat.startForeground(
                            this@PrayerAodService,
                            PrayerNotification.NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                        )

                    } else {

                        ServiceCompat.startForeground(
                            this@PrayerAodService,
                            PrayerNotification.NOTIFICATION_ID,
                            notification,
                            0
                        )
                    }
                }

                // Обновляем примерно в начале следующей минуты.
                val delay =
                    60_000L -
                        (System.currentTimeMillis() % 60_000L) +
                        250L

                handler.postDelayed(
                    this,
                    delay
                )
            }
        }

    override fun onCreate() {
        super.onCreate()

        handler.post(updater)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        return START_STICKY
    }

    override fun onDestroy() {

        handler.removeCallbacks(updater)

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent?
    ): IBinder? = null
}
