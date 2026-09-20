package com.islam9lam.namazwidget
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.time.YearMonth
object Sources {
 private fun times(text:String)=Regex("""\b([0-2]?\d:[0-5]\d)\b""").findAll(text).map{it.value.padStart(5,'0')}.toList()
 fun fetch(city:String,ym:YearMonth):List<PrayerDay>{
  val url=if(city=="Грозный")
   "https://govzalla.com/ламазан-хенаш-время-молитв?city="+URLEncoder.encode(city,"UTF-8")+"&month="+ym.monthValue
  else "https://umma.ru/raspisanie-namaza/"+URLEncoder.encode(city.lowercase().replace(" ","-"),"UTF-8")
  val doc=Jsoup.connect(url).userAgent("Mozilla/5.0 NamazWidget/0.1").timeout(15000).get()
  val out=mutableListOf<PrayerDay>()
  doc.select("tr").forEach { tr ->
   val cells=tr.select("td").map{it.text().trim()}
   val day=cells.firstOrNull()?.filter{it.isDigit()}?.toIntOrNull()
   if(day!=null){val ts=times(cells.drop(1).joinToString(" "));if(ts.size>=6) out+=PrayerDay(day,ts[0],ts[1],ts[2],ts[3],ts[4],ts[5])}
  }
  return out.distinctBy{it.day}.sortedBy{it.day}
 }
}
