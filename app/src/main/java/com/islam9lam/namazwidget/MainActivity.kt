package com.islam9lam.namazwidget
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.work.*
import java.util.concurrent.TimeUnit
class MainActivity:AppCompatActivity(){
 override fun onCreate(b:Bundle?){super.onCreate(b)
  val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(40,70,40,40)}
  val title=TextView(this).apply{text="Время намаза";textSize=28f}
  val note=TextView(this).apply{text="Москва по умолчанию. Города — Umma.ru, Грозный — Govzalla. Расписание сохраняется офлайн.";textSize=16f}
  val spinner=Spinner(this); val names=Cities.list.map{it.name}
  spinner.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,names)
  spinner.setSelection(names.indexOf(Store.city(this)).coerceAtLeast(0))
  val sync=Button(this).apply{text="Сохранить город и обновить расписание"}
  val status=TextView(this).apply{text="Текущий и следующий месяц кэшируются для работы без интернета."}
  box.addView(title);box.addView(note);box.addView(spinner);box.addView(sync);box.addView(status);setContentView(box)
  sync.setOnClickListener{Store.setCity(this,names[spinner.selectedItemPosition]);WorkManager.getInstance(this).enqueue(OneTimeWorkRequestBuilder<SyncWorker>().build());status.text="Загрузка…"}
  WorkManager.getInstance(this).enqueueUniquePeriodicWork("sync",ExistingPeriodicWorkPolicy.KEEP,PeriodicWorkRequestBuilder<SyncWorker>(3,TimeUnit.DAYS).build())
  WorkManager.getInstance(this).enqueue(OneTimeWorkRequestBuilder<SyncWorker>().build())
 }
}
