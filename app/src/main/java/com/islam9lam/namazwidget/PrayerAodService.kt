package com.islam9lam.namazwidget

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class PrayerAodService:Service(){
 private val minuteHandler=Handler(Looper.getMainLooper())
 private var minuteRefreshScheduled=false
 private val minuteRefresh=object:Runnable{
  override fun run(){
   minuteRefreshScheduled=false
   if(!Store.isAodEnabled(this@PrayerAodService)){
    stopSelf()
    return
   }
   // Only the compact remaining-time label is refreshed here. Seconds in
   // expanded notifications/widgets are rendered by Android's chronometer.
   PrayerAod.refresh(this@PrayerAodService,scheduleNextTransition=false)
   scheduleMinuteRefresh()
  }
 }

 override fun onCreate(){
  super.onCreate()
  if(!Store.isAodEnabled(this)){stopSelf();return}
  PrayerAod.prepareChannel(this)
  val placeholder=NotificationCompat.Builder(this,PrayerAod.CHANNEL_ID)
   .setSmallIcon(R.drawable.ic_prayer_notification)
   .setContentTitle("Время намаза")
   .setContentText("Готовим обратный отсчёт…")
   .setOngoing(true)
   .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
   .build()
  if(Build.VERSION.SDK_INT>=34){
   ServiceCompat.startForeground(this,PrayerAod.NOTIFICATION_ID,placeholder,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
  }else{
   ServiceCompat.startForeground(this,PrayerAod.NOTIFICATION_ID,placeholder,0)
  }
  PrayerAod.refresh(this)
  scheduleMinuteRefresh()
 }

 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
  if(Store.isAodEnabled(this)){
   PrayerAod.refresh(this)
   scheduleMinuteRefresh()
  }else stopSelf()
  return START_STICKY
 }

 override fun onDestroy(){
  minuteHandler.removeCallbacks(minuteRefresh)
  minuteRefreshScheduled=false
  super.onDestroy()
 }

 private fun scheduleMinuteRefresh(){
  if(minuteRefreshScheduled)return
  val delay=60_000L-(System.currentTimeMillis()%60_000L)+250L
  minuteRefreshScheduled=true
  minuteHandler.postDelayed(minuteRefresh,delay)
 }

 override fun onBind(intent:Intent?):IBinder?=null
}
