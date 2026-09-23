package com.islam9lam.namazwidget
import android.content.Context
import org.json.JSONArray
object Store {
 private const val PREF="namaz"
 fun city(c:Context)=c.getSharedPreferences(PREF,0).getString("city","Москва")?:"Москва"
 fun setCity(c:Context,s:String)=c.getSharedPreferences(PREF,0).edit().putString("city",s).apply()
 fun isAodEnabled(c:Context)=c.getSharedPreferences(PREF,0).getBoolean("aod_enabled",false)
 fun setAodEnabled(c:Context,enabled:Boolean)=c.getSharedPreferences(PREF,0).edit().putBoolean("aod_enabled",enabled).apply()
 fun save(c:Context,key:String,days:List<PrayerDay>){
  val a=JSONArray()
  days.forEach{d->a.put(JSONArray(listOf(d.day,d.fajr,d.sunrise,d.dhuhr,d.asr,d.maghrib,d.isha)))}
  c.getSharedPreferences(PREF,0).edit().putString(key,a.toString()).apply()
 }
 fun load(c:Context,key:String):List<PrayerDay> = try {
  val a=JSONArray(c.getSharedPreferences(PREF,0).getString(key,"[]"))
  (0 until a.length()).map{val x=a.getJSONArray(it);PrayerDay(x.getInt(0),x.getString(1),x.getString(2),x.getString(3),x.getString(4),x.getString(5),x.getString(6))}
 } catch(e:Exception){ emptyList() }
}
