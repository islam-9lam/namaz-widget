package com.islam9lam.namazwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.time.Duration
import java.time.YearMonth

class PrayerWidget:AppWidgetProvider(){
 override fun onUpdate(context:Context,manager:AppWidgetManager,ids:IntArray){ids.forEach{PrayerWidgetRenderer.render(context,manager,it,R.layout.widget_prayer_compact)}}
 companion object {
  fun refreshAll(context:Context){
   val manager=AppWidgetManager.getInstance(context)
   manager.getAppWidgetIds(ComponentName(context,PrayerWidget::class.java)).forEach{PrayerWidgetRenderer.render(context,manager,it,R.layout.widget_prayer_compact)}
   manager.getAppWidgetIds(ComponentName(context,PrayerWidgetLarge::class.java)).forEach{PrayerWidgetRenderer.render(context,manager,it,R.layout.widget_prayer)}
  }
 }
}

class PrayerWidgetLarge:AppWidgetProvider(){
 override fun onUpdate(context:Context,manager:AppWidgetManager,ids:IntArray){ids.forEach{PrayerWidgetRenderer.render(context,manager,it,R.layout.widget_prayer)}}
}

private object PrayerWidgetRenderer{
 fun render(context:Context,manager:AppWidgetManager,id:Int,layout:Int){
  val city=Store.selectedCity(context);val now=PrayerTimeline.cityNow(context);val date=now.toLocalDate()
  val day=Store.load(context,"${city.name}-${YearMonth.from(date)}").find{it.day==date.dayOfMonth}
  val state=PrayerTimeline.now(context)
  val views=RemoteViews(context.packageName,layout)
  views.setTextViewText(R.id.city,city.name.uppercase())
  views.setTextViewText(R.id.qibla,"↗ ${Qibla.bearing(city.lat,city.lon)}°")
  if(day==null || state==null){
   views.setTextViewText(R.id.currentName,"НЕТ ДАННЫХ");views.setTextViewText(R.id.currentTime,"—:—")
   views.setTextViewText(R.id.nextName,"ОТКРОЙТЕ ПРИЛОЖЕНИЕ");views.setTextViewText(R.id.nextTime,"");views.setTextViewText(R.id.countdown,"загрузить расписание")
   views.setTextViewText(R.id.others,"")
  }else{
   val minutes=Duration.between(now,state.next.time).toMinutes().coerceAtLeast(0)
   views.setTextViewText(R.id.currentName,"${state.current.name.uppercase()} · СЕЙЧАС")
   views.setTextViewText(R.id.currentTime,state.current.time.toLocalTime().toString())
   views.setTextViewText(R.id.nextName,"СЛЕДУЮЩИЙ · ${state.next.name.uppercase()}")
   views.setTextViewText(R.id.nextTime,state.next.time.toLocalTime().toString())
   views.setTextViewText(R.id.countdown,"через ${minutes/60} ч ${minutes%60} мин")
   val asrLabel=if(Store.asrMethod(context)=="hanafi")"Аср ханафи" else "Аср шафи"
   views.setTextViewText(R.id.others,"Фаджр ${day.fajr} · $asrLabel ${Store.selectedAsr(context,day)} · Иша ${day.isha}")
  }
  val open=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  views.setOnClickPendingIntent(R.id.root,open)
  manager.updateAppWidget(id,views)
 }
}
