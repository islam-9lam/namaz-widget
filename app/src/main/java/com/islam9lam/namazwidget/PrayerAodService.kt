package com.islam9lam.namazwidget

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class PrayerAodService:Service(){
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
 }

 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
  if(Store.isAodEnabled(this))PrayerAod.refresh(this) else stopSelf()
  return START_STICKY
 }

 override fun onBind(intent:Intent?):IBinder?=null
}
