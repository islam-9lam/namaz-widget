package com.islam9lam.namazwidget

import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder

object CitySearch {
 fun search(query:String):List<CitySearchResult>{
  val clean=query.trim()
  if(clean.length<2)return emptyList()
  val encoded=URLEncoder.encode(clean,Charsets.UTF_8.name())
  val body=Jsoup.connect("https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=10&language=ru&format=json")
   .ignoreContentType(true).timeout(15_000).execute().body()
  val results=JSONObject(body).optJSONArray("results")?:return emptyList()
  return (0 until results.length()).mapNotNull{index->
   val item=results.optJSONObject(index)?:return@mapNotNull null
   val name=item.optString("name").trim()
   val country=item.optString("country").trim()
   val zone=item.optString("timezone").trim()
   if(name.isBlank()||zone.isBlank()||!item.has("latitude")||!item.has("longitude"))null
   else CitySearchResult(name,country,item.optString("admin1").trim().ifBlank{null},item.getDouble("latitude"),item.getDouble("longitude"),zone)
  }
 }
}
