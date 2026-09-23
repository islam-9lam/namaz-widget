package com.islam9lam.namazwidget

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.Surface
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.abs
import kotlin.math.roundToInt

class QiblaActivity:AppCompatActivity(),SensorEventListener{
 private lateinit var sensorManager:SensorManager
 private lateinit var locationManager:LocationManager
 private var rotationSensor:Sensor?=null
 private lateinit var arrow:TextView
 private lateinit var heading:TextView
 private lateinit var status:TextView
 private lateinit var bearingLabel:TextView
 private var qiblaBearing=0
 private var currentRotation=0f

 private val permission=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){values->
  if(values.values.any{it})findLocation() else useSelectedCity("Геолокация не разрешена")
 }

 override fun onCreate(state:Bundle?){
  super.onCreate(state)
  WindowCompat.setDecorFitsSystemWindows(window,false)
  window.statusBarColor=Color.rgb(7,25,22);window.navigationBarColor=Color.rgb(7,25,22)
  sensorManager=getSystemService(Context.SENSOR_SERVICE) as SensorManager
  locationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager
  rotationSensor=sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
  buildScreen();requestLocation()
 }

 private fun buildScreen(){
  val bg=Color.rgb(7,25,22);val surface=Color.rgb(14,42,36);val primary=Color.rgb(242,247,245);val secondary=Color.rgb(174,201,193);val gold=Color.rgb(225,190,105)
  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;setPadding(dp(24),dp(30),dp(24),dp(30));setBackgroundColor(bg)}
  ViewCompat.setOnApplyWindowInsetsListener(root){view,insets->
   val bars=insets.getInsets(WindowInsetsCompat.Type.systemBars())
   view.setPadding(dp(24),dp(30)+bars.top,dp(24),dp(30)+bars.bottom)
   insets
  }
  val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
  top.addView(Button(this).apply{text="←";textSize=20f;isAllCaps=false;setOnClickListener{finish()}})
  top.addView(label("Кыбла",28f,primary,true),LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f).apply{leftMargin=dp(12)})
  root.addView(top,LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT))
  root.addView(space(28))
  bearingLabel=label("Определяю направление…",15f,secondary).apply{gravity=Gravity.CENTER}
  root.addView(bearingLabel)
  root.addView(space(24))
  val compass=FrameLayout(this).apply{background=GradientDrawable().apply{shape=GradientDrawable.OVAL;setColor(surface);setStroke(dp(1),Color.rgb(45,90,78))};elevation=dp(3).toFloat()}
  arrow=TextView(this).apply{text="↑";textSize=105f;gravity=Gravity.CENTER;setTextColor(gold)}
  compass.addView(arrow,FrameLayout.LayoutParams(dp(240),dp(240),Gravity.CENTER))
  compass.addView(label("С",15f,secondary,true).apply{gravity=Gravity.CENTER},FrameLayout.LayoutParams(dp(40),dp(40),Gravity.TOP or Gravity.CENTER_HORIZONTAL))
  root.addView(compass,LinearLayout.LayoutParams(dp(270),dp(270)))
  root.addView(space(26))
  heading=label(if(rotationSensor==null)"Датчик компаса недоступен" else "Подготовка компаса…",19f,primary,true).apply{gravity=Gravity.CENTER}
  root.addView(heading)
  status=label("Поворачивайте телефон плавно",14f,secondary).apply{gravity=Gravity.CENTER;setPadding(0,dp(10),0,0)}
  root.addView(status)
  setContentView(root)
 }

 private fun requestLocation(){
  val fine=ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
  val coarse=ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
  if(fine||coarse)findLocation() else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION))
 }

 private fun findLocation(){
  val fine=ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
  val coarse=ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
  if(!fine&&!coarse){useSelectedCity("Нет доступа к геолокации");return}
  val last=runCatching{locationManager.getProviders(true).mapNotNull{provider->runCatching{locationManager.getLastKnownLocation(provider)}.getOrNull()}.maxByOrNull{it.time}}.getOrNull()
  if(last!=null)useLocation(last) else{
   val provider=when{locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)->LocationManager.GPS_PROVIDER;locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)->LocationManager.NETWORK_PROVIDER;else->null}
   if(provider==null){useSelectedCity("Геолокация выключена")}
   else runCatching{
    if(Build.VERSION.SDK_INT>=30){
     locationManager.getCurrentLocation(provider,null,mainExecutor){location->if(location!=null)useLocation(location) else useSelectedCity("Местоположение не найдено")}
    }else{
     @Suppress("DEPRECATION")
     locationManager.requestSingleUpdate(provider,android.location.LocationListener{location->useLocation(location)},null)
    }
   }.onFailure{useSelectedCity("Не удалось получить координаты")}
  }
 }

 private fun useLocation(location:Location){
  qiblaBearing=Qibla.bearing(location.latitude,location.longitude)
  bearingLabel.text="Направление Кыблы: $qiblaBearing° · по текущей геопозиции"
 }

 private fun useSelectedCity(reason:String){
  val city=Store.selectedCity(this);qiblaBearing=Qibla.bearing(city.lat,city.lon)
  bearingLabel.text="Направление Кыблы: $qiblaBearing° · ${city.name}"
  status.text="$reason · использую координаты выбранного города"
 }

 override fun onResume(){super.onResume();rotationSensor?.let{sensorManager.registerListener(this,it,SensorManager.SENSOR_DELAY_UI)}}
 override fun onPause(){sensorManager.unregisterListener(this);super.onPause()}

 override fun onSensorChanged(event:SensorEvent){
  if(event.sensor.type!=Sensor.TYPE_ROTATION_VECTOR)return
  val raw=FloatArray(9);SensorManager.getRotationMatrixFromVector(raw,event.values)
  val adjusted=FloatArray(9)
  val screenRotation=if(Build.VERSION.SDK_INT>=30)display?.rotation?:Surface.ROTATION_0 else @Suppress("DEPRECATION") windowManager.defaultDisplay.rotation
  when(screenRotation){
   Surface.ROTATION_90->SensorManager.remapCoordinateSystem(raw,SensorManager.AXIS_Y,SensorManager.AXIS_MINUS_X,adjusted)
   Surface.ROTATION_180->SensorManager.remapCoordinateSystem(raw,SensorManager.AXIS_MINUS_X,SensorManager.AXIS_MINUS_Y,adjusted)
   Surface.ROTATION_270->SensorManager.remapCoordinateSystem(raw,SensorManager.AXIS_MINUS_Y,SensorManager.AXIS_X,adjusted)
   else->System.arraycopy(raw,0,adjusted,0,9)
  }
  val orientation=FloatArray(3);SensorManager.getOrientation(adjusted,orientation)
  val azimuth=(Math.toDegrees(orientation[0].toDouble()).toFloat()+360f)%360f
  val target=(qiblaBearing-azimuth+360f)%360f
  var delta=target-(currentRotation%360f);if(delta>180f)delta-=360f;if(delta< -180f)delta+=360f
  currentRotation+=delta*.18f;arrow.rotation=currentRotation
  val difference=abs(((qiblaBearing-azimuth+540f)%360f)-180f)
  heading.text=if(difference<=4f)"Направление Кыблы ✓" else "Кыбла · $qiblaBearing°"
  status.text="Направление телефона: ${azimuth.roundToInt()}°"
 }

 override fun onAccuracyChanged(sensor:Sensor?,accuracy:Int)=Unit
 private fun label(value:String,size:Float,color:Int,bold:Boolean=false)=TextView(this).apply{text=value;textSize=size;setTextColor(color);if(bold)setTypeface(typeface,Typeface.BOLD)}
 private fun space(height:Int)=Space(this).apply{layoutParams=LinearLayout.LayoutParams(1,dp(height))}
 private fun dp(value:Int)=(value*resources.displayMetrics.density+.5f).toInt()
}
