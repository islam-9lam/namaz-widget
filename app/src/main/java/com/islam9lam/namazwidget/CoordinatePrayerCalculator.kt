package com.islam9lam.namazwidget

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.HighLatitudeRule
import com.batoulapps.adhan.Madhab
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

object CoordinatePrayerCalculator {

    private val timeFormatter =
        DateTimeFormatter.ofPattern("HH:mm")

    fun calculate(
        city: City,
        ym: YearMonth
    ): List<PrayerDay> {

        val coordinates =
            Coordinates(
                city.lat,
                city.lon
            )

        val zone =
            ZoneId.of(city.timeZone)

        return (1..ym.lengthOfMonth()).map { day ->

            val date =
                DateComponents(
                    ym.year,
                    ym.monthValue,
                    day
                )

            // Общий расчёт + Аср по Шафи'и.
            val shafiParameters =
                CalculationMethod
                    .MUSLIM_WORLD_LEAGUE
                    .parameters

            shafiParameters.madhab =
                Madhab.SHAFI

            shafiParameters.highLatitudeRule =
                HighLatitudeRule.TWILIGHT_ANGLE

            val shafi =
                PrayerTimes(
                    coordinates,
                    date,
                    shafiParameters
                )

            // Второй расчёт нужен только для времени Асра
            // по ханафитскому мазхабу.
            val hanafiParameters =
                CalculationMethod
                    .MUSLIM_WORLD_LEAGUE
                    .parameters

            hanafiParameters.madhab =
                Madhab.HANAFI

            hanafiParameters.highLatitudeRule =
                HighLatitudeRule.TWILIGHT_ANGLE

            val hanafi =
                PrayerTimes(
                    coordinates,
                    date,
                    hanafiParameters
                )

            fun format(value: Date): String =
                Instant
                    .ofEpochMilli(value.time)
                    .atZone(zone)
                    .format(timeFormatter)

            PrayerDay(
                day = day,
                fajr = format(shafi.fajr),
                sunrise = format(shafi.sunrise),
                dhuhr = format(shafi.dhuhr),

                // В координатном режиме:
                // asr = Шафи'и
                // asrHanafi = Ханафи
                asr = format(shafi.asr),

                maghrib = format(shafi.maghrib),
                isha = format(shafi.isha),

                asrHanafi = format(hanafi.asr)
            )
        }
    }
}
