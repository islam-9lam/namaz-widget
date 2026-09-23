package com.islam9lam.namazwidget

import android.content.Context
import java.time.*

data class ClassicPrayerMoment(
    val name: String,
    val time: String
)

data class ClassicPrayerState(
    val city: String,
    val nextPrayer: ClassicPrayerMoment,
    val nextDateTime: LocalDateTime,
    val currentPrayer: ClassicPrayerMoment?,
    val currentLabel: String,
    val hint: String
)

object PrayerStateCalculator {

    fun calculate(
        context: Context,
        now: LocalDateTime = CityClock.localDateTime(context)
    ): ClassicPrayerState? {

        val city = Store.city(context)
        val date = now.toLocalDate()
        val ym = YearMonth.from(date)

        val today = Store.load(
            context,
            "$city-$ym"
        ).firstOrNull {
            it.day == date.dayOfMonth
        } ?: return null

        // Для официальных источников asrHanafi == null,
        // поэтому всегда используется Аср самого источника.
        //
        // Для координатного расчёта пользователь может
        // выбрать Шафи'и или Ханафи.
        val activeAsr =
            if (
                today.asrHanafi != null &&
                Store.asrMethod(context) == "hanafi"
            ) {
                today.asrHanafi
            } else {
                today.asr
            }

        val prayers = listOf(
            ClassicPrayerMoment("Фаджр", today.fajr),
            ClassicPrayerMoment("Зухр", today.dhuhr),
            ClassicPrayerMoment("Аср", activeAsr),
            ClassicPrayerMoment("Магриб", today.maghrib),
            ClassicPrayerMoment("Иша", today.isha)
        )

        val nextToday = prayers.firstOrNull {
            LocalTime.parse(it.time) > now.toLocalTime()
        }

        val nextPrayer: ClassicPrayerMoment
        val nextDateTime: LocalDateTime

        if (nextToday != null) {

            nextPrayer = nextToday

            nextDateTime = LocalDateTime.of(
                date,
                LocalTime.parse(nextToday.time)
            )

        } else {

            val tomorrow = date.plusDays(1)

            val tomorrowData = Store.load(
                context,
                "$city-${YearMonth.from(tomorrow)}"
            ).firstOrNull {
                it.day == tomorrow.dayOfMonth
            }

            val fajr =
                tomorrowData?.fajr ?: today.fajr

            nextPrayer =
                ClassicPrayerMoment("Фаджр", fajr)

            nextDateTime =
                LocalDateTime.of(
                    tomorrow,
                    LocalTime.parse(fajr)
                )
        }

        val sunrise =
            LocalTime.parse(today.sunrise)

        val fajr =
            LocalTime.parse(today.fajr)

        val dhuhr =
            LocalTime.parse(today.dhuhr)

        val asr =
            LocalTime.parse(activeAsr)

        val maghrib =
            LocalTime.parse(today.maghrib)

        val isha =
            LocalTime.parse(today.isha)

        val time = now.toLocalTime()

        val currentPrayer: ClassicPrayerMoment? = when {
            time >= isha ->
                ClassicPrayerMoment("Иша", today.isha)

            time >= maghrib ->
                ClassicPrayerMoment("Магриб", today.maghrib)

            time >= asr ->
                ClassicPrayerMoment("Аср", activeAsr)

            time >= dhuhr ->
                ClassicPrayerMoment("Зухр", today.dhuhr)

            time >= fajr && time < sunrise ->
                ClassicPrayerMoment("Фаджр", today.fajr)

            else -> null
        }

        val label: String
        val hint: String

        when {

            time >= fajr && time < sunrise -> {
                label = "Время Фаджра"

                hint =
                    "Фаджр можно совершить до восхода · ${today.sunrise}"
            }

            time >= sunrise &&
                time < sunrise.plusMinutes(20) -> {

                val end =
                    sunrise.plusMinutes(20)

                label =
                    "Период после восхода"

                hint =
                    "Расчётный период после восхода · до $end"
            }

            time >= sunrise.plusMinutes(20) &&
                time < dhuhr.minusMinutes(10) -> {

                label = "До Зухра"

                hint =
                    "Следующий обязательный намаз — Зухр"
            }

            time >= dhuhr.minusMinutes(10) &&
                time < dhuhr &&
                date.dayOfWeek != DayOfWeek.FRIDAY -> {

                label =
                    "Период перед Зухром"

                hint =
                    "Зухр начнётся в ${today.dhuhr}"
            }

            time >= dhuhr &&
                time < asr -> {

                label =
                    "Время Зухра"

                hint =
                    "Следующий обязательный намаз — Аср · $activeAsr"
            }

            time >= asr &&
                time < maghrib.minusMinutes(20) -> {

                label =
                    "Время Асра"

                hint =
                    "Аср продолжается до захода солнца"
            }

            time >= maghrib.minusMinutes(20) &&
                time < maghrib -> {

                label =
                    "Позднее время Асра"

                hint =
                    "Если Аср ещё не совершён, его можно совершить сейчас · Магриб ${today.maghrib}"
            }

            time >= maghrib &&
                time < isha -> {

                label =
                    "Время Магриба"

                hint =
                    "Следующий обязательный намаз — Иша · ${today.isha}"
            }

            time >= isha -> {

                label =
                    "Время Иша"

                hint =
                    "Следующий обязательный намаз — Фаджр"
            }

            else -> {

                label =
                    "До Фаджра"

                hint =
                    "Фаджр начнётся в ${today.fajr}"
            }
        }

        return ClassicPrayerState(
            city,
            nextPrayer,
            nextDateTime,
            currentPrayer,
            label,
            hint
        )
    }
}
