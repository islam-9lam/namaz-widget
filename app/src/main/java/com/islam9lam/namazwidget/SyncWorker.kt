package com.islam9lam.namazwidget

import android.content.Context
import androidx.work.*
import java.time.YearMonth

class SyncWorker(
    ctx: Context,
    p: WorkerParameters
) : Worker(ctx, p) {

    override fun doWork(): Result = try {

        val cityObj =
            CityClock.city(applicationContext)

        val city =
            cityObj.name

        // Текущий месяц определяем по часовому поясу
        // выбранного города, а не телефона.
        val ym = YearMonth.from(
            CityClock.date(applicationContext)
        )

        // Передаём сам объект City.
        // Это важно для произвольных городов:
        // координаты и timezone берутся из сохранённого City.
        val result =
            Sources.fetch(cityObj, ym)

        Store.save(
            applicationContext,
            "$city-$ym",
            result.days
        )

        val saved = result.days.size
        val source = result.source

        val cityNow =
            CityClock.now(applicationContext)

        val cachedUntil =
            ym.atEndOfMonth()
                .toString()

        applicationContext
            .getSharedPreferences("namaz", 0)
            .edit()
            .putString(
                "source",
                source
            )
            .putString(
                "last_successful_update",
                cityNow.toString()
            )
            .putString(
                "cached_until",
                cachedUntil
            )
            .apply()

        Store.setSyncStatus(
            applicationContext,
            "Готово: $city. " +
                "$saved дней. " +
                "Источник: $source. " +
                "Офлайн до $cachedUntil"
        )

        Store.markSourcesRestored(applicationContext)

        PrayerWidget.refreshAll(
            applicationContext
        )

        Result.success()

    } catch (e: Exception) {

        // Старый кэш не удаляем.
        // Если сети нет, сохранённое расписание
        // текущего месяца продолжает работать.
        Store.setSyncStatus(
            applicationContext,
            "Не удалось обновить: " +
                (e.message ?: e.javaClass.simpleName)
        )

        Result.retry()
    }
}
