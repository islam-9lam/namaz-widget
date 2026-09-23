package com.islam9lam.namazwidget

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LiveData
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity:AppCompatActivity(){
 private val backgroundColor=Color.rgb(7,25,22)
 private val surface=Color.rgb(14,42,36)
 private val gold=Color.rgb(225,190,105)
 private val primary=Color.rgb(242,247,245)
 private val secondary=Color.rgb(174,201,193)
 private val green=Color.rgb(72,171,142)
 private val danger=Color.rgb(239,150,139)
 private lateinit var aod:Switch
 private lateinit var promotion:TextView
 private lateinit var syncButton:TextView
 private lateinit var syncStatus:TextView
 private lateinit var aodStatus:TextView
 private lateinit var cityPicker:TextView
 private lateinit var cityMeta:TextView
 private lateinit var schedulePreview:TextView
 private lateinit var asrPicker:TextView
 private lateinit var qiblaRow:TextView
 private lateinit var selectedCity:City
 private var enableAodAfterPermission=false

 override fun onCreate(state:Bundle?){
 super.onCreate(state)
  WindowCompat.setDecorFitsSystemWindows(window,false)
  window.statusBarColor=backgroundColor
  window.navigationBarColor=backgroundColor
  selectedCity=Store.selectedCity(this)
  setContentView(buildScreen())
  scheduleRefresh()
  updateSchedulePreview()
  if(todayData()?.asrHanafi==null)startSync()
 }

 private fun buildScreen():View{
  val scroll=ScrollView(this).apply{isFillViewport=true;setBackgroundColor(backgroundColor)}
  val content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(24),dp(32),dp(24),dp(36))}
  ViewCompat.setOnApplyWindowInsetsListener(content){view,insets->
   val bars=insets.getInsets(WindowInsetsCompat.Type.systemBars())
   view.setPadding(dp(24),dp(32)+bars.top,dp(24),dp(36)+bars.bottom)
   insets
  }
  content.addView(text("NAMAZ WIDGET",12f,gold,true).apply{letterSpacing=.16f})
  content.addView(text("Время намаза",34f,primary,true).apply{setPadding(0,dp(5),0,0)})
  content.addView(text("Расчёт по координатам работает офлайн.",15f,secondary).apply{setPadding(0,dp(6),0,dp(30))})

  content.addView(sectionLabel("ГОРОД И РАСПИСАНИЕ"))
  cityPicker=text("${selectedCity.name}  ▾",21f,primary,true).apply{
   setPadding(0,dp(12),0,dp(5));isClickable=true;isFocusable=true;setOnClickListener{showCityMenu()}
  }
  content.addView(cityPicker,ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT))
  cityMeta=text(cityDescription(),13f,secondary).apply{setPadding(0,0,0,dp(12))}
  content.addView(cityMeta)
  content.addView(View(this).apply{setBackgroundColor(Color.rgb(40,76,67))},LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dp(1)))
  syncButton=text("Пересчитать расписание",16f,backgroundColor,true).apply{
   gravity=Gravity.CENTER;setPadding(dp(18),dp(14),dp(18),dp(14));background=rounded(gold,18);setOnClickListener{startSync()}
  }
  content.addView(syncButton,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT).apply{topMargin=dp(18)})
  syncStatus=text(scheduleStatus(),13f,secondary).apply{setPadding(dp(2),dp(11),0,0)}
  content.addView(syncStatus)

  content.addView(sectionLabel("СЕГОДНЯ").apply{setPadding(0,dp(30),0,dp(10))})
  val scheduleCard=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(16),dp(18),dp(16));background=rounded(surface,22,Color.rgb(34,75,65),1)}
  schedulePreview=text("",16f,primary,true).apply{setLineSpacing(dp(6).toFloat(),1f)}
  scheduleCard.addView(schedulePreview)
  asrPicker=text("",14f,gold,true).apply{
   setPadding(0,dp(15),0,dp(3));isClickable=true;isFocusable=true;setOnClickListener{showAsrMenu()}
  }
  scheduleCard.addView(asrPicker)
  qiblaRow=text("",14f,secondary).apply{setPadding(0,dp(8),0,dp(4));isClickable=true;isFocusable=true;setOnClickListener{startActivity(Intent(this@MainActivity,QiblaActivity::class.java))}}
  scheduleCard.addView(qiblaRow)
  content.addView(scheduleCard)

  content.addView(sectionLabel("ЭКРАН БЛОКИРОВКИ").apply{setPadding(0,dp(32),0,dp(10))})
  val aodCard=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(16),dp(18),dp(16));background=rounded(surface,22,Color.rgb(34,75,65),1)}
  val aodRow=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
  aodRow.addView(text("Следующий намаз",18f,primary,true),LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f))
  aod=Switch(this).apply{
   showText=false;isClickable=true;isFocusable=true;isChecked=Store.isAodEnabled(this@MainActivity)
   val states=arrayOf(intArrayOf(android.R.attr.state_checked),intArrayOf())
   thumbTintList=ColorStateList(states,intArrayOf(Color.WHITE,secondary))
   trackTintList=ColorStateList(states,intArrayOf(green,Color.rgb(56,85,77)))
  }
  aodRow.addView(aod)
  aodCard.addView(aodRow)
  aodCard.addView(text("AOD · NOW BAR · LIVE UPDATES",12f,gold,true).apply{setPadding(0,dp(3),0,0);letterSpacing=.06f})
  aodCard.addView(text("Название, время и системный обратный отсчёт до следующего намаза.",14f,secondary).apply{setPadding(0,dp(8),0,0)})
  aodStatus=text(PrayerAod.statusText(this),13f,secondary).apply{setPadding(0,dp(12),0,0)}
  aodCard.addView(aodStatus)
  promotion=text("Открыть настройки Live-уведомлений",14f,gold,true).apply{
   visibility=View.GONE;gravity=Gravity.CENTER;setPadding(dp(12),dp(12),dp(12),dp(12));background=rounded(Color.TRANSPARENT,14,gold,1)
   setOnClickListener{openPromotionSettings()}
  }
  aodCard.addView(promotion,LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT).apply{topMargin=dp(12)})
  content.addView(aodCard)

  aod.setOnCheckedChangeListener{_,checked->
   if(checked)requestAodPermissionOrEnable() else{PrayerAod.disable(this);updateAodStatus()}
  }
  scroll.addView(content,ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT))
  return scroll
 }

 private fun showCityMenu(){
  val menu=PopupMenu(this,cityPicker)
  menu.menu.add(0,SEARCH_CITY_ID,0,"⌕  Найти любой город…")
  Cities.list.forEachIndexed{index,city->menu.menu.add(0,index,index+1,city.name)}
  menu.setOnMenuItemClickListener{item->
   if(item.itemId==SEARCH_CITY_ID)showCitySearch()
   else selectCity(Cities.list[item.itemId],false)
   true
  }
  menu.show()
 }

 private fun showCitySearch(){
  val input=EditText(this).apply{
   hint="Например: Берлин";setSingleLine(true);imeOptions=EditorInfo.IME_ACTION_SEARCH
   setPadding(dp(20),dp(8),dp(20),dp(8))
  }
  val dialog=AlertDialog.Builder(this).setTitle("Найти город").setMessage("Можно выбрать город в любой стране. Интернет нужен только для поиска.")
   .setView(input).setPositiveButton("Найти",null).setNegativeButton("Отмена",null).create()
  dialog.setOnShowListener{
   val button=dialog.getButton(AlertDialog.BUTTON_POSITIVE)
   fun search(){
    val query=input.text.toString().trim()
    if(query.length<2){input.error="Введите хотя бы 2 буквы";return}
    button.isEnabled=false;button.text="Ищу…"
    Thread{
     val result=runCatching{CitySearch.search(query)}
     runOnUiThread{
      if(isFinishing||isDestroyed)return@runOnUiThread
      button.isEnabled=true;button.text="Найти"
      result.onSuccess{cities->
       if(cities.isEmpty())input.error="Ничего не найдено"
       else{dialog.dismiss();showCityResults(cities)}
      }.onFailure{input.error="Не удалось выполнить поиск. Проверьте интернет"}
     }
    }.start()
   }
   button.setOnClickListener{search()}
   input.setOnEditorActionListener{_,action,_->if(action==EditorInfo.IME_ACTION_SEARCH){search();true}else false}
  }
  dialog.show()
 }

 private fun showCityResults(results:List<CitySearchResult>){
  AlertDialog.Builder(this).setTitle("Выберите город").setItems(results.map{it.displayName()}.toTypedArray()){_,which->
   val found=results[which]
   val builtIn=Cities.list.firstOrNull{it.name.equals(found.name,true)&&it.country.equals(found.country,true)}
   selectCity(builtIn?:found.toCity(),builtIn==null)
  }.setNegativeButton("Отмена",null).show()
 }

 private fun selectCity(city:City,custom:Boolean){
  selectedCity=city
  if(custom)Store.setCustomCity(this,city) else Store.setCity(this,city.name)
  cityPicker.text="${city.name}  ▾";cityMeta.text=cityDescription();updateSchedulePreview();startSync()
 }

 private fun showAsrMenu(){
  val menu=PopupMenu(this,asrPicker)
  menu.menu.add(0,0,0,"Шафиитский · тень 1×")
  menu.menu.add(0,1,1,"Ханафитский · тень 2×")
  menu.setOnMenuItemClickListener{item->
   Store.setAsrMethod(this,if(item.itemId==1)"hanafi" else "shafi")
   updateSchedulePreview();PrayerWidget.refreshAll(this);PrayerAod.refresh(this);true
  }
  menu.show()
 }

 private fun startSync(){
  setSyncLoading(true)
  val request=OneTimeWorkRequestBuilder<SyncWorker>().setInputData(workDataOf(SyncWorker.KEY_MANUAL to true)).build()
  val manager=WorkManager.getInstance(this)
  manager.enqueueUniqueWork("manual_sync",ExistingWorkPolicy.REPLACE,request)
  val live:LiveData<WorkInfo?> =manager.getWorkInfoByIdLiveData(request.id)
  live.observe(this){info->
   if(info==null)return@observe
   when(info.state){
    WorkInfo.State.SUCCEEDED->{syncStatus.text=info.outputData.getString(SyncWorker.KEY_MESSAGE)?:scheduleStatus();syncStatus.setTextColor(green);setSyncLoading(false);updateSchedulePreview();updateAodStatus();live.removeObservers(this)}
    WorkInfo.State.FAILED->{syncStatus.text=info.outputData.getString(SyncWorker.KEY_MESSAGE)?:"Не удалось рассчитать расписание";syncStatus.setTextColor(danger);setSyncLoading(false);live.removeObservers(this)}
    WorkInfo.State.CANCELLED->{syncStatus.text="Расчёт отменён";syncStatus.setTextColor(secondary);setSyncLoading(false);live.removeObservers(this)}
    else->{syncStatus.text="Рассчитываю расписание на устройстве…";syncStatus.setTextColor(gold)}
   }
  }
 }

 private fun setSyncLoading(loading:Boolean){
  syncButton.isEnabled=!loading;syncButton.alpha=if(loading).65f else 1f;syncButton.text=if(loading)"Рассчитываю…" else "Пересчитать расписание"
 }

 private fun scheduleRefresh(){
  WorkManager.getInstance(this).enqueueUniquePeriodicWork("sync",ExistingPeriodicWorkPolicy.UPDATE,PeriodicWorkRequestBuilder<SyncWorker>(1,TimeUnit.DAYS).build())
 }

 private fun todayData():PrayerDay?{
  val date=LocalDate.now(ZoneId.of(selectedCity.timeZone))
  return Store.load(this,"${selectedCity.name}-${YearMonth.from(date)}").find{it.day==date.dayOfMonth}
 }

 private fun scheduleStatus():String{
  val date=LocalDate.now(ZoneId.of(selectedCity.timeZone));val count=Store.load(this,"${selectedCity.name}-${YearMonth.from(date)}").size
  if(count==0)return "Расписание ещё не рассчитано"
  val month=YearMonth.from(date).month.getDisplayName(TextStyle.FULL_STANDALONE,Locale("ru"))
  return "Готово · $count дней за $month · работает офлайн"
 }

 private fun updateSchedulePreview(){
  if(!::schedulePreview.isInitialized)return
  val day=todayData()
  if(day==null){
   schedulePreview.text="Фаджр  —:—\nВосход  —:—\nЗухр  —:—\nАср  —:—\nМагриб  —:—\nИша  —:—"
  }else{
   schedulePreview.text="Фаджр   ${day.fajr}\nВосход  ${day.sunrise}\nЗухр      ${day.dhuhr}\nАср шафи   ${day.asr}\nАср ханафи  ${day.asrHanafi?:day.asr}\nМагриб   ${day.maghrib}\nИша         ${day.isha}"
  }
  val hanafi=Store.asrMethod(this)=="hanafi"
  asrPicker.text="В виджете и AOD: Аср ${if(hanafi)"ханафи" else "шафи"}  ▾"
  qiblaRow.text="Кыбла из ${selectedCity.name}:  ${Qibla.bearing(selectedCity.lat,selectedCity.lon)}°  ›"
  syncStatus.text=scheduleStatus()
 }

 private fun cityDescription()="${selectedCity.country.ifBlank{"Выбранный город"}} · MWL · ${selectedCity.timeZone}"

 override fun onResume(){
  super.onResume()
  if(::promotion.isInitialized){
   if(Store.isAodEnabled(this))PrayerAod.refresh(this)
   updatePromotionButton();updateAodStatus()
  }
 }

 private fun requestAodPermissionOrEnable(){
  if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
   enableAodAfterPermission=true;requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),42)
  }else enableAod()
 }

 private fun enableAod(){PrayerAod.enable(this);updateAodStatus();updatePromotionButton()}
 private fun updateAodStatus(){if(::aodStatus.isInitialized){aodStatus.text=PrayerAod.statusText(this);aodStatus.setTextColor(if(PrayerAod.deliveryState(this).posted)green else secondary)}}
 private fun promotionIntent()=Intent("android.settings.APP_NOTIFICATION_PROMOTION_SETTINGS").putExtra("android.provider.extra.APP_PACKAGE",packageName)

 private fun updatePromotionButton(){
  val state=PrayerAod.deliveryState(this)
  val supported=Build.VERSION.SDK_INT>=36&&promotionIntent().resolveActivity(packageManager)!=null
  promotion.visibility=if(supported&&!state.liveUpdatesAllowed)View.VISIBLE else View.GONE
 }

 private fun openPromotionSettings(){
  val intent=promotionIntent()
  if(intent.resolveActivity(packageManager)!=null)startActivity(intent)
  else startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,packageName))
 }

 override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray){
  super.onRequestPermissionsResult(requestCode,permissions,grantResults)
  if(requestCode==42&&enableAodAfterPermission){
   enableAodAfterPermission=false
   if(grantResults.firstOrNull()==PackageManager.PERMISSION_GRANTED)enableAod()
   else{aod.isChecked=false;updateAodStatus()}
  }
 }

 private fun sectionLabel(value:String)=text(value,12f,gold,true).apply{letterSpacing=.12f}
 private fun text(value:String,size:Float,color:Int,bold:Boolean=false)=TextView(this).apply{
  text=value;textSize=size;setTextColor(color);includeFontPadding=false;if(bold)setTypeface(typeface,Typeface.BOLD)
 }
 private fun rounded(fill:Int,radius:Int,stroke:Int=Color.TRANSPARENT,strokeWidth:Int=0)=GradientDrawable().apply{
  shape=GradientDrawable.RECTANGLE;setColor(fill);cornerRadius=dp(radius).toFloat();if(strokeWidth>0)setStroke(dp(strokeWidth),stroke)
 }
 private fun dp(value:Int)=(value*resources.displayMetrics.density+.5f).toInt()

 companion object{private const val SEARCH_CITY_ID=10_000}
}
