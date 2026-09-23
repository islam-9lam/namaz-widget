package com.islam9lam.namazwidget
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import java.util.concurrent.TimeUnit
class MainActivity:AppCompatActivity(){
 private lateinit var aod:Switch
 private lateinit var promotion:Button
 private lateinit var status:TextView
 private var enableAodAfterPermission=false
 override fun onCreate(b:Bundle?){super.onCreate(b)
  val pad=(24*resources.displayMetrics.density).toInt()
  val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(pad,pad*2,pad,pad);setBackgroundColor(Color.rgb(12,24,23))}
  val title=TextView(this).apply{text="Время намаза";textSize=30f;setTextColor(Color.WHITE);setTypeface(typeface,Typeface.BOLD)}
  val note=TextView(this).apply{text="Расписание хранится на устройстве и работает без интернета.";textSize=15f;setTextColor(Color.rgb(181,204,198));setPadding(0,8,0,24)}
  val spinner=Spinner(this); val names=Cities.list.map{it.name}
  spinner.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,names)
  spinner.setSelection(names.indexOf(Store.city(this)).coerceAtLeast(0))
  val sync=Button(this).apply{text="Сохранить город и обновить"}
  val aodTitle=TextView(this).apply{text="НА ЭКРАНЕ БЛОКИРОВКИ";textSize=12f;setTextColor(Color.rgb(221,186,107));setPadding(0,28,0,4)}
  aod=Switch(this).apply{
   text="Следующий намаз · AOD / Now Bar";textSize=17f;setTextColor(Color.WHITE);showText=false
   isClickable=true;isFocusable=true;isChecked=Store.isAodEnabled(this@MainActivity)
   val states=arrayOf(intArrayOf(android.R.attr.state_checked),intArrayOf())
   thumbTintList=ColorStateList(states,intArrayOf(Color.WHITE,Color.rgb(181,204,198)))
   trackTintList=ColorStateList(states,intArrayOf(Color.rgb(55,134,116),Color.rgb(70,88,84)))
  }
  val aodNote=TextView(this).apply{text="Показывает название, время и живой обратный отсчёт. На Android 16 система может вывести карточку в Live Updates / Now Bar; на старых версиях — как постоянное уведомление.";textSize=14f;setTextColor(Color.rgb(181,204,198));setPadding(0,6,0,12)}
  promotion=Button(this).apply{text="Разрешить Live Updates / Now Bar";visibility=View.GONE}
  status=TextView(this).apply{text=if(Store.isAodEnabled(this@MainActivity))"Показ включён" else "Показ выключен";textSize=14f;setTextColor(Color.rgb(221,186,107));gravity=Gravity.START;setPadding(0,12,0,0)}
  box.addView(title);box.addView(note);box.addView(spinner);box.addView(sync);box.addView(aodTitle);box.addView(aod);box.addView(aodNote);box.addView(promotion);box.addView(status);setContentView(box)
  sync.setOnClickListener{Store.setCity(this,names[spinner.selectedItemPosition]);WorkManager.getInstance(this).enqueue(OneTimeWorkRequestBuilder<SyncWorker>().build());status.text="Загружаю расписание…"}
  aod.setOnCheckedChangeListener{_,checked->if(checked)requestAodPermissionOrEnable() else {PrayerAod.disable(this);status.text="Показ выключен"}}
  promotion.setOnClickListener{openPromotionSettings()}
  WorkManager.getInstance(this).enqueueUniquePeriodicWork("sync",ExistingPeriodicWorkPolicy.KEEP,PeriodicWorkRequestBuilder<SyncWorker>(3,TimeUnit.DAYS).build())
  WorkManager.getInstance(this).enqueue(OneTimeWorkRequestBuilder<SyncWorker>().build())
 }
 override fun onResume(){super.onResume();if(::promotion.isInitialized)updatePromotionButton()}
 private fun requestAodPermissionOrEnable(){
  if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
   enableAodAfterPermission=true
   requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),42)
  }else enableAod()
 }
 private fun enableAod(){PrayerAod.enable(this);status.text="Показ включён · отсчёт обновляется автоматически"}
 private fun promotionIntent()=Intent("android.settings.APP_NOTIFICATION_PROMOTION_SETTINGS").putExtra("android.provider.extra.APP_PACKAGE",packageName)
 private fun updatePromotionButton(){
  val supported=Build.VERSION.SDK_INT>=36 && promotionIntent().resolveActivity(packageManager)!=null
  promotion.visibility=if(supported && !NotificationManagerCompat.from(this).canPostPromotedNotifications())View.VISIBLE else View.GONE
 }
 private fun openPromotionSettings(){
  val intent=promotionIntent()
  if(intent.resolveActivity(packageManager)!=null)startActivity(intent)
 }
 override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray){
  super.onRequestPermissionsResult(requestCode,permissions,grantResults)
  if(requestCode==42 && enableAodAfterPermission){enableAodAfterPermission=false
   if(grantResults.firstOrNull()==PackageManager.PERMISSION_GRANTED)enableAod() else {aod.isChecked=false;status.text="Без разрешения на уведомления AOD недоступен"}
  }
 }
}
