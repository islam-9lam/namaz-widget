package com.islam9lam.namazwidget
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.*
import android.widget.RemoteViews
import java.time.*
class PrayerWidget:AppWidgetProvider(){
 override fun onUpdate(c:Context,m:AppWidgetManager,ids:IntArray){ids.forEach{render(c,m,it)}}
 companion object {
  fun refreshAll(c:Context){val m=AppWidgetManager.getInstance(c);m.getAppWidgetIds(ComponentName(c,PrayerWidget::class.java)).forEach{render(c,m,it)}}
  private fun render(c:Context,m:AppWidgetManager,id:Int){
   val city=Store.city(c); val date=LocalDate.now()
   val d=Store.load(c,"$city-${YearMonth.from(date)}").find{it.day==date.dayOfMonth}
   val state=PrayerTimeline.now(c)
   val v=RemoteViews(c.packageName,R.layout.widget_prayer); v.setTextViewText(R.id.city,city.uppercase())
   val cc=Cities.list.find{it.name==city}?:Cities.list[0]; v.setTextViewText(R.id.qibla,"↗ КЫБЛА ${Qibla.bearing(cc.lat,cc.lon)}°")
   if(d==null || state==null){
    v.setTextViewText(R.id.currentName,"НЕТ ДАННЫХ");v.setTextViewText(R.id.currentTime,"—:—")
    v.setTextViewText(R.id.nextName,"Открой приложение");v.setTextViewText(R.id.nextTime,"");v.setTextViewText(R.id.countdown,"для загрузки расписания")
   } else {
    val nt=state.next.time.toLocalTime().toString()
    val mins=Duration.between(LocalDateTime.now(),state.next.time).toMinutes().coerceAtLeast(0)
    v.setTextViewText(R.id.currentName,state.current.name.uppercase()+" · СЕЙЧАС");v.setTextViewText(R.id.currentTime,state.current.time.toLocalTime().toString())
    v.setTextViewText(R.id.nextName,state.next.name.uppercase());v.setTextViewText(R.id.nextTime,nt)
    v.setTextViewText(R.id.countdown,"через ${mins/60} ч ${mins%60} мин")
    v.setTextViewText(R.id.others,"Фаджр ${d.fajr} · Зухр ${d.dhuhr} · Иша ${d.isha}")
   }
   val pi=PendingIntent.getActivity(c,0,Intent(c,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE)
   v.setOnClickPendingIntent(R.id.root,pi); m.updateAppWidget(id,v)
  }
 }
}
