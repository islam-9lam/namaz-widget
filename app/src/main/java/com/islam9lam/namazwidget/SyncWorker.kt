package com.islam9lam.namazwidget

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class SyncWorker(ctx:Context,p:WorkerParameters):Worker(ctx,p){
 companion object {const val KEY_MANUAL="manual";const val KEY_MESSAGE="message"}

 override fun doWork():Result=try{
  val city=Store.selectedCity(applicationContext)
  val now=ZonedDateTime.now(ZoneId.of(city.timeZone))
  val month=YearMonth.from(now)
  val current=Sources.fetch(city,month)
  val next=Sources.fetch(city,month.plusMonths(1))
  Store.save(applicationContext,"${city.name}-$month",current)
  Store.save(applicationContext,"${city.name}-${month.plusMonths(1)}",next)
  Store.setLastCalculation(applicationContext,now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
  PrayerWidget.refreshAll(applicationContext)
  PrayerAod.refresh(applicationContext)
  Result.success(workDataOf(KEY_MESSAGE to "Готово · ${current.size+next.size} дней рассчитано офлайн"))
 }catch(e:Exception){
  Result.failure(workDataOf(KEY_MESSAGE to "Ошибка расчёта: ${e.message?:e.javaClass.simpleName}"))
 }
}
