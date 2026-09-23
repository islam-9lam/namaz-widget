package com.islam9lam.namazwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
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
  val state=PrayerTimeline.widgetState(context,now)
  val views=RemoteViews(context.packageName,layout)
  views.setTextViewText(R.id.city,city.name.uppercase())
  if(day==null || state==null){
   views.setTextViewText(R.id.currentLabel,"Откройте приложение")
   views.setTextViewText(R.id.nextName,"НЕТ ДАННЫХ");views.setTextViewText(R.id.nextTime,"")
   views.setTextViewText(R.id.currentPrayer,"");views.setTextViewText(R.id.hint,"Загрузите расписание")
   views.setChronometer(R.id.countdown,SystemClock.elapsedRealtime(),null,false)
   if(layout==R.layout.widget_prayer){views.setTextViewText(R.id.others,"");views.setTextViewText(R.id.qibla,"")}
  }else{
   views.setTextViewText(R.id.currentLabel,state.label)
   views.setTextViewText(R.id.nextName,state.next.name.uppercase())
   views.setTextViewText(R.id.nextTime,state.next.time.toLocalTime().toString())
   views.setTextViewText(R.id.currentPrayer,state.current?.let{"${it.name.uppercase()}  ${it.time.toLocalTime()}"}?:"")
   views.setTextViewText(R.id.hint,state.hint)
   val base=SystemClock.elapsedRealtime()+Duration.between(now,state.next.time).toMillis().coerceAtLeast(0)
   views.setChronometer(R.id.countdown,base,"%s",true)
   views.setChronometerCountDown(R.id.countdown,true)
   if(layout==R.layout.widget_prayer){
    val asrLabel=if(Store.asrMethod(context)=="hanafi")"Аср ханафи" else "Аср шафи"
    views.setTextViewText(R.id.others,"Фаджр ${day.fajr}  ·  Зухр ${day.dhuhr}  ·  $asrLabel ${Store.selectedAsr(context,day)}  ·  Магриб ${day.maghrib}  ·  Иша ${day.isha}")
    views.setTextViewText(R.id.qibla,"Кыбла ${Qibla.bearing(city.lat,city.lon)}° ↗")
   }
  }
  val open=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  views.setOnClickPendingIntent(R.id.root,open)
  manager.updateAppWidget(id,views)
 }
}
