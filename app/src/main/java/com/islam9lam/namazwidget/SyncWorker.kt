package com.islam9lam.namazwidget
import android.content.Context
import androidx.work.*
import java.time.YearMonth
class SyncWorker(ctx:Context,p:WorkerParameters):Worker(ctx,p){
 override fun doWork():Result=try{
  val city=Store.city(applicationContext); val now=YearMonth.now()
  listOf(now,now.plusMonths(1)).forEach{ym->val d=Sources.fetch(city,ym);if(d.isNotEmpty())Store.save(applicationContext,"$city-$ym",d)}
  PrayerWidget.refreshAll(applicationContext); Result.success()
 }catch(e:Exception){Result.retry()}
}
