package com.islam9lam.namazwidget
data class PrayerDay(val day:Int,val fajr:String,val sunrise:String,val dhuhr:String,val asr:String,val maghrib:String,val isha:String)
data class City(val name:String,val lat:Double,val lon:Double)
object Cities {
 val list=listOf(
  City("Москва",55.7558,37.6173), City("Грозный",43.3180,45.6982),
  City("Санкт-Петербург",59.9343,30.3351), City("Казань",55.7961,49.1064),
  City("Уфа",54.7388,55.9721), City("Махачкала",42.9849,47.5047),
  City("Екатеринбург",56.8389,60.6057), City("Самара",53.1959,50.1002),
  City("Краснодар",45.0355,38.9753), City("Ростов-на-Дону",47.2357,39.7015),
  City("Сочи",43.5855,39.7231), City("Ставрополь",45.0428,41.9734)
 )
}
