package com.islam9lam.namazwidget

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

object PrayerAod {
 const val ACTION_UNPIN="com.islam9lam.namazwidget.action.UNPIN_AOD"
 private const val CHANNEL_ID="next_prayer"
 private const val NOTIFICATION_ID=7105
 private const val WORK_NAME="aod_refresh"
 private val timeFormat=DateTimeFormatter.ofPattern("HH:mm")

 fun enable(context:Context){
  Store.setAodEnabled(context,true)
  schedule(context)
  refresh(context)
 }

 fun disable(context:Context){
  Store.setAodEnabled(context,false)
  WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
  NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
 }

 fun refresh(context:Context){
  if(!Store.isAodEnabled(context))return
  if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
  createChannel(context)
  val city=Store.city(context)
  val state=PrayerTimeline.now(context)
  val open=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val unpin=PendingIntent.getBroadcast(context,1,Intent(context,AodActionReceiver::class.java).setAction(ACTION_UNPIN),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val builder=NotificationCompat.Builder(context,CHANNEL_ID)
   .setSmallIcon(R.drawable.ic_prayer_notification)
   .setColor(Color.rgb(55,134,116))
   .setContentIntent(open)
   .setOngoing(true)
   .setOnlyAlertOnce(true)
   .setSilent(true)
   .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
   .setCategory(NotificationCompat.CATEGORY_EVENT)
   .setRequestPromotedOngoing(true)
   .addAction(0,"Убрать",unpin)
  if(state==null){
   builder.setContentTitle("Расписание загружается")
    .setContentText("$city · откройте приложение для обновления")
    .setShortCriticalText("Намаз")
  }else{
   val nextTime=state.next.time.format(timeFormat)
   val endMillis=state.next.time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
   val remaining=Duration.between(LocalDateTime.now(),state.next.time).toMinutes().coerceAtLeast(0)
   builder.setContentTitle("${state.next.name} · $nextTime")
    .setContentText("$city · после ${state.current.name} · осталось ${durationText(remaining)}")
    .setSubText("Следующий намаз")
    .setWhen(endMillis)
    .setShowWhen(true)
    .setUsesChronometer(true)
    .setChronometerCountDown(true)
    .setShortCriticalText(nextTime)
    .setStyle(progressStyle(context,state.progress))
  }
  NotificationManagerCompat.from(context).notify(NOTIFICATION_ID,builder.build())
 }

 private fun progressStyle(context:Context,progress:Int):NotificationCompat.ProgressStyle{
  val gold=Color.rgb(221,186,107)
  val green=Color.rgb(55,134,116)
  return NotificationCompat.ProgressStyle()
   .setStyledByProgress(true)
   .setProgressSegments(listOf(NotificationCompat.ProgressStyle.Segment(100).setColor(green)))
   .setProgressPoints(listOf(NotificationCompat.ProgressStyle.Point(100).setColor(gold)))
   .setProgressTrackerIcon(IconCompat.createWithResource(context,R.drawable.ic_prayer_notification))
   .setProgress(progress.coerceIn(0,100))
 }

 private fun createChannel(context:Context){
  val manager=context.getSystemService(NotificationManager::class.java)
  manager.createNotificationChannel(NotificationChannel(CHANNEL_ID,"Следующий намаз",NotificationManager.IMPORTANCE_DEFAULT).apply{
   description="Обратный отсчёт до следующего намаза на экране блокировки"
   setSound(null,null)
   enableVibration(false)
   lockscreenVisibility=android.app.Notification.VISIBILITY_PUBLIC
  })
 }

 private fun schedule(context:Context){
  WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME,ExistingPeriodicWorkPolicy.UPDATE,PeriodicWorkRequestBuilder<AodRefreshWorker>(15,TimeUnit.MINUTES).build())
 }

 private fun durationText(minutes:Long)=when{
  minutes<60 -> "$minutes мин"
  minutes%60==0L -> "${minutes/60} ч"
  else -> "${minutes/60} ч ${minutes%60} мин"
 }
}

class AodRefreshWorker(context:Context,params:WorkerParameters):Worker(context,params){
 override fun doWork():Result{PrayerAod.refresh(applicationContext);return Result.success()}
}

class AodActionReceiver:BroadcastReceiver(){
 override fun onReceive(context:Context,intent:Intent){if(intent.action==PrayerAod.ACTION_UNPIN)PrayerAod.disable(context)}
}

class AodRestoreReceiver:BroadcastReceiver(){
 override fun onReceive(context:Context,intent:Intent){
  if(intent.action in setOf(Intent.ACTION_BOOT_COMPLETED,Intent.ACTION_DATE_CHANGED,Intent.ACTION_TIME_CHANGED,Intent.ACTION_TIMEZONE_CHANGED) && Store.isAodEnabled(context))PrayerAod.enable(context)
 }
}
