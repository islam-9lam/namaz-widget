package com.islam9lam.namazwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import java.time.Duration
import java.time.LocalDateTime

class PrayerWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray
    ) {
        ids.forEach { render(context, manager, it) }
    }

    companion object {

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)

            val ids = manager.getAppWidgetIds(
                ComponentName(context, PrayerWidget::class.java)
            )

            ids.forEach { render(context, manager, it) }
        }

        private fun render(
            context: Context,
            manager: AppWidgetManager,
            id: Int
        ) {
            val views = RemoteViews(
                context.packageName,
                R.layout.widget_prayer
            )

            val state = PrayerStateCalculator.calculate(context)

            if (state == null) {
                views.setTextViewText(
                    R.id.city,
                    Store.city(context).uppercase()
                )

                views.setTextViewText(R.id.nextName, "НЕТ ДАННЫХ")
                views.setTextViewText(R.id.nextTime, "")
                views.setTextViewText(R.id.currentLabel, "Откройте приложение")
                views.setTextViewText(R.id.currentPrayer, "")
                views.setTextViewText(R.id.hint, "Загрузите расписание")

                views.setChronometer(
                    R.id.countdown,
                    SystemClock.elapsedRealtime(),
                    null,
                    false
                )
            } else {
                views.setTextViewText(
                    R.id.city,
                    state.city.uppercase()
                )

                views.setTextViewText(
                    R.id.nextName,
                    state.nextPrayer.name.uppercase()
                )

                views.setTextViewText(
                    R.id.nextTime,
                    state.nextPrayer.time
                )

                views.setTextViewText(
                    R.id.currentLabel,
                    state.currentLabel
                )

                val current = state.currentPrayer

                views.setTextViewText(
                    R.id.currentPrayer,
                    if (current != null)
                        "${current.name.uppercase()}  ${current.time}"
                    else
                        ""
                )

                views.setTextViewText(
                    R.id.hint,
                    state.hint
                )

                val remaining = Duration.between(
                    CityClock.localDateTime(context),
                    state.nextDateTime
                ).toMillis().coerceAtLeast(0)

                val base =
                    SystemClock.elapsedRealtime() + remaining

                views.setChronometer(
                    R.id.countdown,
                    base,
                    "%s",
                    true
                )

                views.setChronometerCountDown(
                    R.id.countdown,
                    true
                )

                views.setViewVisibility(
                    R.id.event,
                    View.GONE
                )
            }

            val openApp = PendingIntent.getActivity(
                context,
                100,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(
                R.id.root,
                openApp
            )

            manager.updateAppWidget(id, views)
        }
    }
}
