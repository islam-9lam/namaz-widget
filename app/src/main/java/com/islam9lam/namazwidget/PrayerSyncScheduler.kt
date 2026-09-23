package com.islam9lam.namazwidget

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object PrayerSyncScheduler {

    private const val WORK_NAME =
        "prayer_schedule_periodic_sync"

    fun schedule(context: Context) {

        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(
                    NetworkType.CONNECTED
                )
                .build()

        val request =
            PeriodicWorkRequestBuilder<SyncWorker>(
                24,
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.MINUTES
                )
                .build()

        WorkManager
            .getInstance(context)
            .enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }
}
