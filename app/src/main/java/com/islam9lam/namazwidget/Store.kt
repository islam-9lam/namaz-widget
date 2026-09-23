package com.islam9lam.namazwidget
import android.content.Context
import org.json.JSONArray

object Store {
 private const val PREF="namaz"

 fun city(c:Context)=c.getSharedPreferences(PREF,0)
  .getString("city","Москва")?:"Москва"

 fun selectedCity(c:Context):City=
  customCity(c)?:runCatching{Cities.find(city(c))}.getOrElse{Cities.list.first()}

 fun setCity(c:Context,s:String){
  c.getSharedPreferences(PREF,0)
   .edit()
   .putString("city",s)
   // Если выбран обычный город из встроенного списка,
   // старый динамический город больше не используем.
   .remove("custom_city_name")
   .remove("custom_city_country")
   .remove("custom_city_lat")
   .remove("custom_city_lon")
   .remove("custom_city_timezone")
   .apply()
 }

 fun setCustomCity(c:Context, city:City){
  c.getSharedPreferences(PREF,0)
   .edit()
   .putString("city",city.name)
   .putString("custom_city_name",city.name)
   .putString("custom_city_country",city.country)
   .putString("custom_city_lat",city.lat.toString())
   .putString("custom_city_lon",city.lon.toString())
   .putString("custom_city_timezone",city.timeZone)
   .apply()
 }

 fun customCity(c:Context):City? = try {
  val p=c.getSharedPreferences(PREF,0)

  val name=p.getString("custom_city_name",null)
   ?: return null

  val country=p.getString("custom_city_country","") ?: ""

  val lat=p.getString("custom_city_lat",null)
   ?.toDoubleOrNull()
   ?: return null

  val lon=p.getString("custom_city_lon",null)
   ?.toDoubleOrNull()
   ?: return null

  val zone=p.getString("custom_city_timezone",null)
   ?: return null

  City(
   name=name,
   country=country,
   lat=lat,
   lon=lon,
   timeZone=zone,
   ummaSlug=null
  )
 } catch(e:Exception){
  null
 }

 fun setSyncStatus(c:Context,s:String)=
  c.getSharedPreferences(PREF,0).edit().putString("sync_status",s).apply()

 fun syncStatus(c:Context)=
  c.getSharedPreferences(PREF,0)
   .getString("sync_status","Расписание ещё не обновлялось.")
   ?:"Расписание ещё не обновлялось."

 fun source(c:Context)=
  c.getSharedPreferences(PREF,0)
   .getString("source","") ?: ""

 fun lastSuccessfulUpdate(c:Context)=
  c.getSharedPreferences(PREF,0)
   .getString("last_successful_update","") ?: ""

 fun cachedUntil(c:Context)=
  c.getSharedPreferences(PREF,0)
   .getString("cached_until","") ?: ""

 fun sourcesRestored(c:Context)=
  c.getSharedPreferences(PREF,0).getBoolean("sources_restored_v5",false)

 fun markSourcesRestored(c:Context)=
  c.getSharedPreferences(PREF,0).edit().putBoolean("sources_restored_v5",true).apply()

 // Используется только для расписаний,
 // рассчитанных по координатам.
 //
 // Возможные значения:
 // "shafi"  — Аср по Шафи'и
 // "hanafi" — Аср по Ханафи
 //
 // Официальные источники эту настройку игнорируют.
 fun asrMethod(c:Context)=
  c.getSharedPreferences(PREF,0)
   .getString("asr_method","shafi") ?: "shafi"

 fun setAsrMethod(c:Context,s:String)=
  c.getSharedPreferences(PREF,0)
   .edit()
   .putString(
    "asr_method",
    if(s=="hanafi") "hanafi" else "shafi"
   )
   .apply()

 fun selectedAsr(c:Context,day:PrayerDay)=
  if(asrMethod(c)=="hanafi") day.asrHanafi?:day.asr else day.asr

 fun isAodEnabled(c:Context)=
  c.getSharedPreferences(PREF,0).getBoolean("aod_enabled",false)

 fun setAodEnabled(c:Context,enabled:Boolean)=
  c.getSharedPreferences(PREF,0).edit().putBoolean("aod_enabled",enabled).apply()

 fun setLastCalculation(c:Context,value:String)=
  c.getSharedPreferences(PREF,0).edit().putString("last_calculation",value).apply()

 fun lastCalculation(c:Context)=
  c.getSharedPreferences(PREF,0).getString("last_calculation","")?:""

 fun remove(c:Context,key:String)=
  c.getSharedPreferences(PREF,0).edit().remove(key).apply()

 fun save(c:Context,key:String,days:List<PrayerDay>){
  val a=JSONArray()
  days.forEach{d->
   a.put(JSONArray(listOf(
    d.day,
    d.fajr,
    d.sunrise,
    d.dhuhr,
    d.asr,
    d.maghrib,
    d.isha,
    d.asrHanafi ?: ""
   )))
  }
  c.getSharedPreferences(PREF,0).edit().putString(key,a.toString()).apply()
 }

 fun load(c:Context,key:String):List<PrayerDay> = try {
  val a=JSONArray(c.getSharedPreferences(PREF,0).getString(key,"[]"))
  (0 until a.length()).map{
   val x=a.getJSONArray(it)
   PrayerDay(
    day = x.getInt(0),
    fajr = x.getString(1),
    sunrise = x.getString(2),
    dhuhr = x.getString(3),
    asr = x.getString(4),
    maghrib = x.getString(5),
    isha = x.getString(6),

    // Старый кэш содержит только 7 элементов.
    // Поэтому восьмое поле читаем только если оно существует.
    asrHanafi =
        if (x.length() > 7) {
            x.optString(7)
                .takeIf { it.isNotBlank() }
        } else {
            null
        }
   )
  }
 } catch(e:Exception){
  emptyList()
 }
}
