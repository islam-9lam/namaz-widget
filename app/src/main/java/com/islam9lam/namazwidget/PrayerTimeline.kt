package com.islam9lam.namazwidget

import android.content.Context
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

data class PrayerMoment(val name:String,val time:LocalDateTime)
data class PrayerState(val current:PrayerMoment,val next:PrayerMoment,val progress:Int)

object PrayerTimeline {
 private val names=listOf("Фаджр","Зухр","Аср","Магриб","Иша")

 fun now(context:Context,at:LocalDateTime=cityNow(context)):PrayerState?{
  val city=Store.selectedCity(context)
  fun stored(date:LocalDate)=Store.load(context,"${city.name}-${YearMonth.from(date)}").find{it.day==date.dayOfMonth}
  return calculate(at,stored(at.toLocalDate().minusDays(1)),stored(at.toLocalDate()),stored(at.toLocalDate().plusDays(1)),Store.asrMethod(context)=="hanafi")
 }

 fun cityNow(context:Context)=LocalDateTime.now(ZoneId.of(Store.selectedCity(context).timeZone))

 fun calculate(at:LocalDateTime,yesterday:PrayerDay?,today:PrayerDay?,tomorrow:PrayerDay?,hanafi:Boolean=false):PrayerState?{
  today?:return null
  val date=at.toLocalDate()
  val todayTimes=times(today,hanafi)
  val index=todayTimes.indexOfLast{!at.toLocalTime().isBefore(it)}
  val current:PrayerMoment
  val next:PrayerMoment
  when {
   index<0 -> {
    current=PrayerMoment(names.last(),LocalDateTime.of(date.minusDays(1),LocalTime.parse((yesterday?:today).isha)))
    next=PrayerMoment(names.first(),LocalDateTime.of(date,todayTimes.first()))
   }
   index==todayTimes.lastIndex -> {
    current=PrayerMoment(names.last(),LocalDateTime.of(date,todayTimes.last()))
    next=PrayerMoment(names.first(),LocalDateTime.of(date.plusDays(1),LocalTime.parse((tomorrow?:today).fajr)))
   }
   else -> {
    current=PrayerMoment(names[index],LocalDateTime.of(date,todayTimes[index]))
    next=PrayerMoment(names[index+1],LocalDateTime.of(date,todayTimes[index+1]))
   }
  }
  val total=Duration.between(current.time,next.time).toMinutes().coerceAtLeast(1)
  val elapsed=Duration.between(current.time,at).toMinutes().coerceIn(0,total)
  return PrayerState(current,next,((elapsed*100)/total).toInt().coerceIn(0,100))
 }

 private fun times(day:PrayerDay,hanafi:Boolean)=listOf(day.fajr,day.dhuhr,if(hanafi)day.asrHanafi?:day.asr else day.asr,day.maghrib,day.isha).map(LocalTime::parse)
}
