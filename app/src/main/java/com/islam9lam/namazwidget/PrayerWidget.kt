package com.islam9lam.namazwidget
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.*
import android.widget.RemoteViews
import java.time.*
import java.time.temporal.ChronoUnit
class PrayerWidget:AppWidgetProvider(){
 override fun onUpdate(c:Context,m:AppWidgetManager,ids:IntArray){ids.forEach{render(c,m,it)}}
 companion object {
  fun refreshAll(c:Context){val m=AppWidgetManager.getInstance(c);m.getAppWidgetIds(ComponentName(c,PrayerWidget::class.java)).forEach{render(c,m,it)}}
  private fun render(c:Context,m:AppWidgetManager,id:Int){
   val city=Store.city(c); val date=LocalDate.now()
   val d=Store.load(c,"$city-${YearMonth.from(date)}").find{it.day==date.dayOfMonth}
   val v=RemoteViews(c.packageName,R.layout.widget_prayer); v.setTextViewText(R.id.city,city.uppercase())
   val cc=Cities.list.find{it.name==city}?:Cities.list[0]; v.setTextViewText(R.id.qibla,"↗ КЫБЛА ${Qibla.bearing(cc.lat,cc.lon)}°")
   if(d==null){
    v.setTextViewText(R.id.currentName,"НЕТ ДАННЫХ");v.setTextViewText(R.id.currentTime,"—:—")
    v.setTextViewText(R.id.nextName,"Открой приложение");v.setTextViewText(R.id.nextTime,"");v.setTextViewText(R.id.countdown,"для загрузки расписания")
   } else {
    val names=listOf("ФАДЖР","ЗУХР","АСР","МАГРИБ","ИША"); val ts=listOf(d.fajr,d.dhuhr,d.asr,d.maghrib,d.isha)
    val now=LocalTime.now(); val parsed=ts.map{LocalTime.parse(it)}
    var cur=parsed.indexOfLast{!now.isBefore(it)}; if(cur<0)cur=4
    val next=if(cur==4)0 else cur+1; val nextDate=if(cur==4)date.plusDays(1)else date
    val nextDay=if(cur==4)Store.load(c,"$city-${YearMonth.from(nextDate)}").find{it.day==nextDate.dayOfMonth}else d
    val nt=if(cur==4)nextDay?.fajr?:d.fajr else ts[next]
    val mins=ChronoUnit.MINUTES.between(LocalDateTime.of(date,now),LocalDateTime.of(nextDate,LocalTime.parse(nt))).coerceAtLeast(0)
    v.setTextViewText(R.id.currentName,names[cur]+" · СЕЙЧАС");v.setTextViewText(R.id.currentTime,ts[cur])
    v.setTextViewText(R.id.nextName,names[next]);v.setTextViewText(R.id.nextTime,nt)
    v.setTextViewText(R.id.countdown,"через ${mins/60} ч ${mins%60} мин")
    v.setTextViewText(R.id.others,"Фаджр ${d.fajr} · Зухр ${d.dhuhr} · Иша ${d.isha}")
   }
   val pi=PendingIntent.getActivity(c,0,Intent(c,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE)
   v.setOnClickPendingIntent(R.id.root,pi); m.updateAppWidget(id,v)
  }
 }
}
