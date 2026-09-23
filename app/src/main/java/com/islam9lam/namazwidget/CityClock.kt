package com.islam9lam.namazwidget

import android.content.Context
import java.time.*

object CityClock {

    fun city(context: Context): City =
        Store.customCity(context)
            ?: Cities.find(Store.city(context))

    fun zone(context: Context): ZoneId =
        ZoneId.of(city(context).timeZone)

    fun now(context: Context): ZonedDateTime =
        ZonedDateTime.now(zone(context))

    fun date(context: Context): LocalDate =
        now(context).toLocalDate()

    fun localDateTime(context: Context): LocalDateTime =
        now(context).toLocalDateTime()
}
