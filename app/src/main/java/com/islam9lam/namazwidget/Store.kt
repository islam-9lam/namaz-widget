package com.islam9lam.namazwidget

import android.content.Context
import org.json.JSONArray

object Store {
 private const val PREF="namaz"
 private fun prefs(c:Context)=c.getSharedPreferences(PREF,0)

 fun city(c:Context)=prefs(c).getString("city","Москва")?:"Москва"
 fun selectedCity(c:Context)=customCity(c)?:Cities.find(city(c))?:Cities.list.first()

 fun setCity(c:Context,s:String){
  prefs(c).edit().putString("city",s)
   .remove("custom_city_name").remove("custom_city_country")
   .remove("custom_city_lat").remove("custom_city_lon").remove("custom_city_timezone").apply()
 }

 fun setCustomCity(c:Context,city:City){
  prefs(c).edit().putString("city",city.name)
   .putString("custom_city_name",city.name).putString("custom_city_country",city.country)
   .putString("custom_city_lat",city.lat.toString()).putString("custom_city_lon",city.lon.toString())
   .putString("custom_city_timezone",city.timeZone).apply()
 }

 fun customCity(c:Context):City?{
  return try{
   val p=prefs(c)
   val name=p.getString("custom_city_name",null)?:return null
   City(name,p.getString("custom_city_country","")?:"",p.getString("custom_city_lat",null)?.toDoubleOrNull()?:return null,
    p.getString("custom_city_lon",null)?.toDoubleOrNull()?:return null,p.getString("custom_city_timezone",null)?:return null)
  }catch(_:Exception){null}
 }

 fun asrMethod(c:Context)=prefs(c).getString("asr_method","shafi")?:"shafi"
 fun setAsrMethod(c:Context,value:String)=prefs(c).edit().putString("asr_method",if(value=="hanafi")"hanafi" else "shafi").apply()
 fun selectedAsr(c:Context,day:PrayerDay)=if(asrMethod(c)=="hanafi")day.asrHanafi?:day.asr else day.asr

 fun isAodEnabled(c:Context)=prefs(c).getBoolean("aod_enabled",false)
 fun setAodEnabled(c:Context,enabled:Boolean)=prefs(c).edit().putBoolean("aod_enabled",enabled).apply()
 fun setLastCalculation(c:Context,value:String)=prefs(c).edit().putString("last_calculation",value).apply()
 fun lastCalculation(c:Context)=prefs(c).getString("last_calculation","")?:""
 fun remove(c:Context,key:String)=prefs(c).edit().remove(key).apply()

 fun save(c:Context,key:String,days:List<PrayerDay>){
  val values=JSONArray()
  days.forEach{day->values.put(JSONArray(listOf(day.day,day.fajr,day.sunrise,day.dhuhr,day.asr,day.maghrib,day.isha,day.asrHanafi?:"")))}
  prefs(c).edit().putString(key,values.toString()).apply()
 }

 fun load(c:Context,key:String):List<PrayerDay> = try {
  val values=JSONArray(prefs(c).getString(key,"[]"))
  (0 until values.length()).map{index->
   val item=values.getJSONArray(index)
   PrayerDay(item.getInt(0),item.getString(1),item.getString(2),item.getString(3),item.getString(4),item.getString(5),item.getString(6),
    item.optString(7).takeIf{it.isNotBlank()})
  }
 } catch(_:Exception){emptyList()}
}
