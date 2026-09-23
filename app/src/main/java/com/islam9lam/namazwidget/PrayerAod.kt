package com.islam9lam.namazwidget

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

data class AodDeliveryState(
 val notificationsAllowed:Boolean,
 val channelAllowed:Boolean,
 val posted:Boolean,
 val liveUpdatesAllowed:Boolean,
 val promoted:Boolean
)

object PrayerAod {
 const val ACTION_UNPIN="com.islam9lam.namazwidget.action.UNPIN_AOD"
 internal const val CHANNEL_ID="next_prayer"
 internal const val NOTIFICATION_ID=7105
 private const val PERIODIC_WORK="aod_refresh"
 private const val TRANSITION_WORK="aod_transition"
 private val timeFormat=DateTimeFormatter.ofPattern("HH:mm")

 fun enable(context:Context){
  Store.setAodEnabled(context,true)
  schedulePeriodic(context)
  runCatching{
   ContextCompat.startForegroundService(context,Intent(context,PrayerAodService::class.java))
  }.onFailure{
   // Some Android builds reject a foreground-service launch from a boot/time
   // broadcast. The promoted notification can still be refreshed directly.
   refresh(context)
  }
 }

 fun disable(context:Context){
  Store.setAodEnabled(context,false)
  Store.setLiveSettingsPrompted(context,false)
  WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK)
  WorkManager.getInstance(context).cancelUniqueWork(TRANSITION_WORK)
  context.stopService(Intent(context,PrayerAodService::class.java))
  NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
 }

 fun openPromotionSettingsIfNeeded(activity:Activity,force:Boolean=false):Boolean{
  if(Build.VERSION.SDK_INT<36)return false
  if(NotificationManagerCompat.from(activity).canPostPromotedNotifications())return false
  if(!force && Store.liveSettingsPrompted(activity))return false
  val intent=Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS)
   .putExtra(Settings.EXTRA_APP_PACKAGE,activity.packageName)
  if(intent.resolveActivity(activity.packageManager)==null)return false
  Store.setLiveSettingsPrompted(activity,true)
  activity.startActivity(intent)
  return true
 }

 fun refresh(context:Context):Boolean{
  if(!Store.isAodEnabled(context))return false
  if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return false
  prepareChannel(context)
  val manager=context.getSystemService(NotificationManager::class.java)
  if(manager.getNotificationChannel(CHANNEL_ID)?.importance==NotificationManager.IMPORTANCE_NONE)return false
  val city=Store.selectedCity(context)
  val state=PrayerTimeline.now(context)
  val open=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val unpin=PendingIntent.getBroadcast(context,1,Intent(context,AodActionReceiver::class.java).setAction(ACTION_UNPIN),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val builder=NotificationCompat.Builder(context,CHANNEL_ID)
   .setSmallIcon(R.drawable.ic_prayer_notification)
   .setColor(Color.rgb(55,134,116))
   .setContentIntent(open)
   .setOngoing(true)
   .setOnlyAlertOnce(true)
   .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
   .setPriority(NotificationCompat.PRIORITY_DEFAULT)
   .setRequestPromotedOngoing(true)
   .addAction(0,"Убрать",unpin)
  if(state==null){
   builder.setContentTitle("Расписание рассчитывается")
    .setContentText("${city.name} · откройте приложение")
    .setShortCriticalText("Намаз")
  }else{
   val nextTime=state.next.time.format(timeFormat)
   val endMillis=state.next.time.atZone(ZoneId.of(city.timeZone)).toInstant().toEpochMilli()
   val remaining=Duration.between(PrayerTimeline.cityNow(context),state.next.time).toMinutes().coerceAtLeast(0)
   builder.setContentTitle("${state.next.name} · $nextTime")
    .setContentText("${city.name} · после ${state.current.name} · осталось ${durationText(remaining)}")
    .setSubText("Следующий намаз")
    .setWhen(endMillis)
    .setShowWhen(true)
    .setUsesChronometer(true)
    .setChronometerCountDown(true)
    .setShortCriticalText(nextTime)
    .setStyle(progressStyle(context,state.progress))
   scheduleTransition(context,remaining)
  }
  NotificationManagerCompat.from(context).notify(NOTIFICATION_ID,builder.build())
  return true
 }

 fun deliveryState(context:Context):AodDeliveryState{
  val compat=NotificationManagerCompat.from(context)
  val notificationsAllowed=compat.areNotificationsEnabled() && (Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)
  val manager=context.getSystemService(NotificationManager::class.java)
  val channelAllowed=Build.VERSION.SDK_INT<26 || manager.getNotificationChannel(CHANNEL_ID)?.importance!=NotificationManager.IMPORTANCE_NONE
  val active=if(Build.VERSION.SDK_INT>=23)manager.activeNotifications.firstOrNull{it.id==NOTIFICATION_ID}?.notification else null
  val liveAllowed=Build.VERSION.SDK_INT<36 || compat.canPostPromotedNotifications()
  val promoted=active?.let{notification->
   runCatching{
    val flag=Notification::class.java.getField("FLAG_PROMOTED_ONGOING").getInt(null)
    notification.flags and flag !=0
   }.getOrDefault(false)
  }?:false
  return AodDeliveryState(notificationsAllowed,channelAllowed,active!=null,liveAllowed,promoted)
 }

 fun statusText(context:Context):String{
  val state=deliveryState(context)
  return when{
   !Store.isAodEnabled(context)->"Показ выключен"
   !state.notificationsAllowed->"Нужно разрешить уведомления приложения"
   !state.channelAllowed->"Канал «Следующий намаз» выключен в настройках"
   !state.posted->"Уведомление не создано — нажмите переключатель ещё раз"
   Build.VERSION.SDK_INT>=36 && !state.liveUpdatesAllowed->"Уведомление работает · разрешите Live-уведомления для Now Bar"
   state.promoted->"Live Update активен · карточка отправлена в Now Bar"
   Build.VERSION.SDK_INT>=36->"Уведомление отправлено · Samsung пока не повысил его до Now Bar"
   else->"Уведомление активно на экране блокировки"
  }
 }

 private fun progressStyle(context:Context,progress:Int)=NotificationCompat.ProgressStyle()
  .setStyledByProgress(true)
  .setProgressSegments(listOf(NotificationCompat.ProgressStyle.Segment(100).setColor(Color.rgb(55,134,116))))
  .setProgressPoints(listOf(NotificationCompat.ProgressStyle.Point(100).setColor(Color.rgb(221,186,107))))
  .setProgressTrackerIcon(IconCompat.createWithResource(context,R.drawable.ic_prayer_notification))
  .setProgress(progress.coerceIn(0,100))

 internal fun prepareChannel(context:Context){
  val manager=context.getSystemService(NotificationManager::class.java)
  manager.createNotificationChannel(NotificationChannel(CHANNEL_ID,"Следующий намаз",NotificationManager.IMPORTANCE_DEFAULT).apply{
   description="Живой обратный отсчёт до следующего намаза"
   setSound(null,null)
   enableVibration(false)
   lockscreenVisibility=Notification.VISIBILITY_PUBLIC
  })
 }

 private fun schedulePeriodic(context:Context){
  WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC_WORK,ExistingPeriodicWorkPolicy.UPDATE,PeriodicWorkRequestBuilder<AodRefreshWorker>(15,TimeUnit.MINUTES).build())
 }

 private fun scheduleTransition(context:Context,remainingMinutes:Long){
  val delay=(remainingMinutes+1).coerceAtLeast(1)
  val request=OneTimeWorkRequestBuilder<AodRefreshWorker>().setInitialDelay(delay,TimeUnit.MINUTES).build()
  WorkManager.getInstance(context).enqueueUniqueWork(TRANSITION_WORK,ExistingWorkPolicy.REPLACE,request)
 }

 private fun durationText(minutes:Long)=when{
  minutes<60->"$minutes мин"
  minutes%60==0L->"${minutes/60} ч"
  else->"${minutes/60} ч ${minutes%60} мин"
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
