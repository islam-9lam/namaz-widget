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
data class WidgetPrayerState(val next:PrayerMoment,val current:PrayerMoment?,val label:String,val hint:String)

object PrayerTimeline {
 private val names=listOf("Фаджр","Зухр","Аср","Магриб","Иша")

 fun now(context:Context,at:LocalDateTime=cityNow(context)):PrayerState?{
  val city=Store.selectedCity(context)
  fun stored(date:LocalDate)=Store.load(context,"${city.name}-${YearMonth.from(date)}").find{it.day==date.dayOfMonth}
  return calculate(at,stored(at.toLocalDate().minusDays(1)),stored(at.toLocalDate()),stored(at.toLocalDate().plusDays(1)),Store.asrMethod(context)=="hanafi")
 }

 fun cityNow(context:Context)=LocalDateTime.now(ZoneId.of(Store.selectedCity(context).timeZone))

 fun widgetState(context:Context,at:LocalDateTime=cityNow(context)):WidgetPrayerState?{
  val timeline=now(context,at)?:return null
  val city=Store.selectedCity(context)
  val date=at.toLocalDate()
  val today=Store.load(context,"${city.name}-${YearMonth.from(date)}").find{it.day==date.dayOfMonth}?:return null
  val fajr=LocalTime.parse(today.fajr)
  val sunrise=LocalTime.parse(today.sunrise)
  val dhuhr=LocalTime.parse(today.dhuhr)
  val asr=LocalTime.parse(Store.selectedAsr(context,today))
  val maghrib=LocalTime.parse(today.maghrib)
  val isha=LocalTime.parse(today.isha)
  val time=at.toLocalTime()
  fun moment(name:String,value:LocalTime)=PrayerMoment(name,LocalDateTime.of(date,value))
  val current=when{
   time>=isha->moment("Иша",isha)
   time>=maghrib->moment("Магриб",maghrib)
   time>=asr->moment("Аср",asr)
   time>=dhuhr->moment("Зухр",dhuhr)
   time>=fajr && time<sunrise->moment("Фаджр",fajr)
   else->null
  }
  val label:String
  val hint:String
  when{
   time>=fajr && time<sunrise->{label="Время Фаджра";hint="Фаджр можно совершить до восхода · ${today.sunrise}"}
   time>=sunrise && time<sunrise.plusMinutes(20)->{label="После восхода";hint="Расчётный период после восхода · до ${sunrise.plusMinutes(20)}"}
   time>=sunrise.plusMinutes(20) && time<dhuhr.minusMinutes(10)->{label="До Зухра";hint="Следующий обязательный намаз — Зухр"}
   time>=dhuhr.minusMinutes(10) && time<dhuhr->{label="Перед Зухром";hint="Зухр начнётся в ${today.dhuhr}"}
   time>=dhuhr && time<asr->{label="Время Зухра";hint="Следующий обязательный намаз — Аср · ${Store.selectedAsr(context,today)}"}
   time>=asr && time<maghrib.minusMinutes(20)->{label="Время Асра";hint="Аср продолжается до захода солнца"}
   time>=maghrib.minusMinutes(20) && time<maghrib->{label="Поздний Аср";hint="Если Аср ещё не совершён · Магриб ${today.maghrib}"}
   time>=maghrib && time<isha->{label="Время Магриба";hint="Следующий обязательный намаз — Иша · ${today.isha}"}
   time>=isha->{label="Время Иша";hint="Следующий обязательный намаз — Фаджр"}
   else->{label="До Фаджра";hint="Фаджр начнётся в ${today.fajr}"}
  }
  return WidgetPrayerState(timeline.next,current,label,hint)
 }

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
